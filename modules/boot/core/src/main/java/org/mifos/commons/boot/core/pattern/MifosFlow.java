///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.pattern;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface MifosFlow<C extends MifosFlow.MifosFlowContext> {
    C run(C context);

    /// Marker interface for context objects
    interface MifosFlowContext extends Serializable {}

    /// A unit-of-work boundary around (part of) a flow, typically a database transaction.
    ///
    /// Keeping this a plain functional interface lets flows declare *where* their transaction starts and ends
    /// without depending on Spring; the starter adapts it to `TransactionOperations`, tests use [#NONE].
    @FunctionalInterface
    interface Boundary {
        Boundary NONE = Supplier::get;

        <T> T run(Supplier<T> work);
    }

    /// Glue between atomic [MifosUsecase]s and composite [MifosFlow]s.
    ///
    /// A flow context is an immutable record; every step derives a usecase request from the context, executes the
    /// usecase and folds the response back into a new context. This keeps usecases unaware of the flows that use
    /// them and keeps flows free of business rules: they only describe *order*, *conditions* and *boundaries*.
    interface Dsl {
        @SafeVarargs
        static <C extends MifosFlowContext> MifosFlow<C> sequence(MifosFlow<C>... steps) {
            return ctx -> {
                for (MifosFlow<C> step : steps) ctx = step.run(ctx);
                return ctx;
            };
        }

        // --- decision ---
        static <C extends MifosFlowContext> MifosFlow<C> when(
                Predicate<C> cond, MifosFlow<C> then, MifosFlow<C> otherwise) {
            return ctx -> cond.test(ctx) ? then.run(ctx) : otherwise.run(ctx);
        }

        static <C extends MifosFlowContext> MifosFlow<C> when(Predicate<C> cond, MifosFlow<C> then) {
            return when(cond, then, noop());
        }

        // --- iteration ---
        static <C extends MifosFlowContext> MifosFlow<C> repeatWhile(Predicate<C> cond, MifosFlow<C> body) {
            return ctx -> {
                while (cond.test(ctx)) ctx = body.run(ctx);
                return ctx;
            };
        }

        static <C extends MifosFlowContext, I> MifosFlow<C> forEach(
                Function<C, ? extends Collection<I>> items, BiConsumer<I, C> body) {
            return ctx -> {
                for (I item : items.apply(ctx)) body.accept(item, ctx);
                return ctx;
            };
        }

        // --- parallel fork/join ---
        static <C extends MifosFlowContext> MifosFlow<C> forkJoin(
                Function<C, C> snapshot, List<MifosFlow<C>> forks, BinaryOperator<C> merge, Executor executor) {
            return ctx -> {
                List<CompletableFuture<C>> results = forks.stream()
                        .map(fork -> CompletableFuture.supplyAsync(() -> fork.run(snapshot.apply(ctx)), executor))
                        .toList();
                C merged = ctx;
                for (CompletableFuture<C> r : results) merged = merge.apply(merged, r.join());
                return merged;
            };
        }

        // --- helpers / decorator example ---
        static <C extends MifosFlowContext> MifosFlow<C> noop() {
            return ctx -> ctx;
        }

        static <C extends MifosFlowContext> MifosFlow<C> retry(int attempts, MifosFlow<C> flow) {
            return ctx -> {
                RuntimeException last = null;
                for (int i = 1; i <= attempts; i++) {
                    try {
                        return flow.run(ctx);
                    } catch (RuntimeException e) {
                        last = e;
                    }
                }
                throw last;
            };
        }

        /// Executes `usecase` with a request derived from the context and merges the response into the context.
        static <C extends MifosFlowContext, REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse> MifosFlow<C> step(
                Function<? super C, ? extends REQ> request,
                MifosUsecase<REQ, RES> usecase,
                BiFunction<? super C, ? super RES, ? extends C> merge) {
            return ctx -> merge.apply(ctx, usecase.execute(request.apply(ctx)));
        }

        /// Executes a verifying usecase (one that throws on violation) and leaves the context unchanged.
        static <C extends MifosFlowContext, REQ extends MifosUsecase.MifosRequest> MifosFlow<C> check(
                Function<? super C, ? extends REQ> request, MifosUsecase<REQ, ?> usecase) {
            return ctx -> {
                usecase.execute(request.apply(ctx));
                return ctx;
            };
        }

        /// Runs a flow with a different context type: `enter` maps into the sub-flow context, `exit` merges back.
        static <C extends MifosFlowContext, D extends MifosFlowContext> MifosFlow<C> subflow(
                Function<? super C, ? extends D> enter,
                MifosFlow<D> flow,
                BiFunction<? super C, ? super D, ? extends C> exit) {
            return ctx -> exit.apply(ctx, flow.run(enter.apply(ctx)));
        }

        /// Runs the flow inside a unit-of-work boundary (e.g. a transaction).
        static <C extends MifosFlowContext> MifosFlow<C> within(MifosFlow.Boundary boundary, MifosFlow<C> flow) {
            return ctx -> boundary.run(() -> flow.run(ctx));
        }

        /// Retries the flow on failures matching `retryable` (e.g. optimistic concurrency conflicts).
        ///
        /// Unlike [#retry] business failures are not retried. Combine with [#within] so that every attempt
        /// runs in a fresh transaction: `retryOn(3, conflict, within(tx, flow))`.
        static <C extends MifosFlowContext> MifosFlow<C> retryOn(
                int attempts, Predicate<? super RuntimeException> retryable, MifosFlow<C> flow) {
            if (attempts < 1) {
                throw new IllegalArgumentException("attempts must be >= 1");
            }

            return ctx -> {
                for (int attempt = 1; ; attempt++) {
                    try {
                        return flow.run(ctx);
                    } catch (RuntimeException e) {
                        if (attempt >= attempts || !retryable.test(e)) {
                            throw e;
                        }
                    }
                }
            };
        }
    }
}
