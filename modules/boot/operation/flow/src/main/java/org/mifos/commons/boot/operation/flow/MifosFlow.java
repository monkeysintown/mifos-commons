///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NonNull;
import org.mifos.commons.boot.operation.flow.exception.MifosFlowException;
import org.mifos.commons.boot.operation.flow.spi.Boundary;
import org.mifos.commons.boot.operation.flow.spi.UsecaseRef;
import org.mifos.commons.boot.operation.usecase.MifosUsecase;
import org.mifos.commons.boot.operation.usecase.MifosUsecase.MifosRequest;
import org.mifos.commons.boot.operation.usecase.MifosUsecase.MifosResponse;

/// An executable flow: a function from one immutable context to the next.
///
/// Dsl are not written by implementing this interface. They are described with
/// [Dsl] as a
/// [Definition] and turned into a `MifosFlow` by a
/// [Engine], which supplies transactions, executors, retry policies,
/// usecases and interceptors.
@FunctionalInterface
public interface MifosFlow<C extends MifosFlow.MifosFlowContext> {
    C run(C context);

    /// Marker interface for context objects
    interface MifosFlowContext extends Serializable {}

    /// Where a flow's unit of work starts and ends, referenced by name (resolved by the [Engine]) or given
    /// directly.
    ///
    /// Flow definitions use names so that they stay free of Spring; the starter maps the well-known names to
    /// `TransactionTemplate`s with the matching propagation.
    sealed interface BoundaryRef {
        /// No boundary. Unlike every other boundary, this one does not count as "inside a boundary" for validation.
        BoundaryRef NONE = new Direct(Boundary.NONE);
        BoundaryRef REQUIRED = new ByName("required");
        BoundaryRef REQUIRES_NEW = new ByName("requires-new");
        BoundaryRef READ_ONLY = new ByName("read-only");

        static BoundaryRef of(String name) {
            return new ByName(name);
        }

        static BoundaryRef of(Boundary boundary) {
            return new Direct(boundary);
        }

        /// Whether this is [#NONE]. Decided by the reference alone, so validation never depends on how the engine
        /// resolves names: a definition that is invalid in production is also invalid in a test.
        default boolean isNone() {
            return this instanceof Direct(Boundary boundary) && boundary == Boundary.NONE;
        }

        record ByName(String name) implements BoundaryRef {
            public ByName {
                requireNonNull(name, "name");
            }

            @Override
            @NonNull public String toString() {
                return name;
            }
        }

        record Direct(Boundary boundary) implements BoundaryRef {
            public Direct {
                requireNonNull(boundary, "boundary");
            }

            @Override
            @NonNull public String toString() {
                return boundary == Boundary.NONE ? "none" : "direct";
            }
        }
    }

    /// The description of a flow: what runs, in which order, under which conditions and boundaries.
    ///
    /// Definitions are immutable data and do not run anything. A [Engine] validates and compiles them into an
    /// executable [MifosFlow], resolving the usecases, boundaries, executors and retry
    /// policies they reference. Build them with the static methods in [Dsl] and the fluent methods below:
    ///
    /// ```java
    /// step(TransferCtx::posting, PostJournalEntry.class, TransferCtx::withEntry)
    ///         .within(BoundaryRef.REQUIRED)
    ///         .retryOn(RetryRef.CONFLICT)
    /// ```
    ///
    /// Each fluent method wraps everything before it, so the code reads in the order of nesting: the retry above
    /// wraps the transaction, which wraps the step.
    sealed interface Definition<C extends MifosFlowContext> {

        // --- fluent composition ---

        /// Runs this, then `next`.
        default Definition<C> andThen(Definition<C> next) {
            return new Sequence<>(List.of(this, next));
        }

        /// Names this part of the flow; [Interceptor]s see the name and error messages include it.
        default Definition<C> named(String name) {
            return new Named<>(name, this);
        }

        /// Runs this inside a unit-of-work boundary, typically a transaction.
        default Definition<C> within(BoundaryRef boundary) {
            return new Within<>(boundary, this);
        }

        /// Retries this according to the referenced policy. Must not be placed inside a boundary.
        default Definition<C> retryOn(RetryRef policy) {
            return new Retry<>(policy, this);
        }

        default Definition<C> retryOn(RetryPolicy policy) {
            return retryOn(RetryRef.of(policy));
        }

        /// Turns failures matching `when` into a context; the handler receives the context this part was started with.
        default Definition<C> recover(
                Predicate<? super RuntimeException> when,
                BiFunction<? super C, ? super RuntimeException, ? extends C> handler) {
            return new Recover<>(this, when, handler);
        }

        // --- nodes ---

        record Noop<C extends MifosFlowContext>() implements Definition<C> {}

        /// An ad-hoc transformation of the context. Prefer [Definition.Step] for anything that is a business rule.
        record Act<C extends MifosFlowContext>(Function<? super C, ? extends C> fn) implements Definition<C> {
            public Act {
                requireNonNull(fn, "fn");
            }
        }

        /// A side effect (e.g. publishing an event) that does not change the context.
        record Tap<C extends MifosFlowContext>(Consumer<? super C> action) implements Definition<C> {
            public Tap {
                requireNonNull(action, "action");
            }
        }

        /// Derives a request from the context, executes the usecase and merges the response into a new context.
        record Step<C extends MifosFlowContext, REQ extends MifosRequest, RES extends MifosResponse>(
                Function<? super C, ? extends REQ> request,
                UsecaseRef<REQ, RES> usecase,
                BiFunction<? super C, ? super RES, ? extends C> merge)
                implements Definition<C> {
            public Step {
                requireNonNull(request, "request");
                requireNonNull(usecase, "usecase");
                requireNonNull(merge, "merge");
            }
        }

        /// Executes a verifying usecase (one that throws on violation) and leaves the context unchanged.
        record Check<C extends MifosFlowContext, REQ extends MifosRequest>(
                Function<? super C, ? extends REQ> request, UsecaseRef<REQ, ?> usecase) implements Definition<C> {
            public Check {
                requireNonNull(request, "request");
                requireNonNull(usecase, "usecase");
            }
        }

        record Sequence<C extends MifosFlowContext>(List<Definition<C>> steps) implements Definition<C> {
            public Sequence {
                steps = List.copyOf(steps);
            }

            @Override
            public Definition<C> andThen(Definition<C> next) {
                var all = new java.util.ArrayList<>(steps);
                all.add(next);
                return new Sequence<>(all);
            }
        }

        record When<C extends MifosFlowContext>(
                Predicate<? super C> condition, Definition<C> then, Definition<C> otherwise) implements Definition<C> {
            public When {
                requireNonNull(condition, "condition");
                requireNonNull(then, "then");
                requireNonNull(otherwise, "otherwise");
            }
        }

        /// Runs `body` while `condition` holds, failing after `maxIterations` iterations instead of looping forever.
        record RepeatWhile<C extends MifosFlowContext>(
                Predicate<? super C> condition, int maxIterations, Definition<C> body) implements Definition<C> {
            public RepeatWhile {
                requireNonNull(condition, "condition");
                requireNonNull(body, "body");
                if (maxIterations < 1) {
                    throw new IllegalArgumentException("maxIterations must be >= 1");
                }
            }
        }

        /// For every item: `ctx = body(enter(ctx, item))`. With a [Definition.Noop] body this is a plain fold.
        record ForEach<C extends MifosFlowContext, I>(
                Function<? super C, ? extends Iterable<? extends I>> items,
                BiFunction<? super C, ? super I, ? extends C> enter,
                Definition<C> body)
                implements Definition<C> {
            public ForEach {
                requireNonNull(items, "items");
                requireNonNull(enter, "enter");
                requireNonNull(body, "body");
            }
        }

        /// Runs a flow with a different context type: `enter` maps into it, `exit` merges back.
        record Subflow<C extends MifosFlowContext, D extends MifosFlowContext>(
                Function<? super C, ? extends D> enter,
                Definition<D> body,
                BiFunction<? super C, ? super D, ? extends C> exit)
                implements Definition<C> {
            public Subflow {
                requireNonNull(enter, "enter");
                requireNonNull(body, "body");
                requireNonNull(exit, "exit");
            }
        }

        /// Runs `forks` in parallel, each on its own `snapshot` of the context, and merges the results in declaration
        /// order. Must not be placed inside a boundary: forks run on other threads and would escape it.
        record ForkJoin<C extends MifosFlowContext>(
                Function<? super C, ? extends C> snapshot,
                List<Definition<C>> forks,
                BinaryOperator<C> merge,
                ExecutorRef executor)
                implements Definition<C> {
            public ForkJoin {
                requireNonNull(snapshot, "snapshot");
                forks = List.copyOf(forks);
                requireNonNull(merge, "merge");
                requireNonNull(executor, "executor");
            }
        }

        record Within<C extends MifosFlowContext>(BoundaryRef boundary, Definition<C> body) implements Definition<C> {
            public Within {
                requireNonNull(boundary, "boundary");
                requireNonNull(body, "body");
            }
        }

        record Retry<C extends MifosFlowContext>(RetryRef policy, Definition<C> body) implements Definition<C> {
            public Retry {
                requireNonNull(policy, "policy");
                requireNonNull(body, "body");
            }
        }

        /// Recovering inside a boundary does not undo a rollback-only mark the failure may have left on the
        /// transaction; recover outside the boundary when the failure came from the database.
        record Recover<C extends MifosFlowContext>(
                Definition<C> body,
                Predicate<? super RuntimeException> when,
                BiFunction<? super C, ? super RuntimeException, ? extends C> handler)
                implements Definition<C> {
            public Recover {
                requireNonNull(body, "body");
                requireNonNull(when, "when");
                requireNonNull(handler, "handler");
            }
        }

        /// Runs the actions in order; if one fails, the undo steps of the completed actions run in reverse order.
        record Saga<C extends MifosFlowContext>(List<Definition.Compensable<C>> steps) implements Definition<C> {
            public Saga {
                steps = List.copyOf(steps);
            }
        }

        /// An action paired with the step that undoes it. Not a flow on its own; see [Saga].
        record Compensable<C extends MifosFlowContext>(Definition<C> action, Definition<C> undo) {
            public Compensable {
                requireNonNull(action, "action");
                requireNonNull(undo, "undo");
            }
        }

        record Named<C extends MifosFlowContext>(String name, Definition<C> body) implements Definition<C> {
            public Named {
                requireNonNull(name, "name");
                requireNonNull(body, "body");
            }
        }
    }

    /// Turns [Definition]s into executable [MifosFlow]s.
    ///
    /// The engine is the replaceable runtime: it decides what a transaction, an executor, a retry policy or a
    /// usecase reference means, and which [Interceptor]s wrap each step. In Spring Boot it is a bean that
    /// applications
    /// can customize or replace.
    interface Engine {

        /// Binds a flow that owns its transactions; same as `bind(definition, Placement.STANDALONE)`.
        default <C extends MifosFlowContext> MifosFlow<C> bind(Definition<C> definition) {
            return bind(definition, Placement.STANDALONE);
        }

        /// Validates `definition` and compiles it into an executable flow that runs at the given `placement`.
        ///
        /// All references are resolved and all structural rules are checked here, once, so problems surface at
        /// startup. Running the result has no interpretation overhead and can be repeated any number of times.
        ///
        /// @throws MifosFlowException if a rule is violated or a reference cannot
        /// be resolved
        <C extends MifosFlowContext> MifosFlow<C> bind(Definition<C> definition, Placement placement);
    }

    /// The executor that runs [Definition.ForkJoin] forks, referenced by name or given directly.
    sealed interface ExecutorRef {
        ExecutorRef DEFAULT = new ByName("default");

        static ExecutorRef of(String name) {
            return new ByName(name);
        }

        static ExecutorRef of(Executor executor) {
            return new Direct(executor);
        }

        record ByName(String name) implements ExecutorRef {
            public ByName {
                requireNonNull(name, "name");
            }

            @Override
            @NonNull public String toString() {
                return name;
            }
        }

        record Direct(Executor executor) implements ExecutorRef {
            public Direct {
                requireNonNull(executor, "executor");
            }

            @Override
            @NonNull public String toString() {
                return "direct";
            }
        }
    }

    /// Around-advice for steps, checks and named parts of a flow, for metrics, tracing, audit, MDC and similar.
    ///
    /// Interceptors are attached when a flow is bound, so they also apply inside fork/join forks without any
    /// thread-bound state. The first interceptor registered with the engine is the outermost.
    ///
    /// Unlike a passive listener, an interceptor can change the outcome (e.g. reject an unauthorized step), so an
    /// exception it throws propagates. Observability interceptors should therefore not throw.
    interface Interceptor {

        default <C extends MifosFlowContext> C intercept(Node node, C context, MifosFlow<C> next) {
            return next.run(context);
        }

        /// Called after `failedAttempt` of a retry failed with a retryable `cause`, before waiting `delay`.
        default void onRetry(Node node, int failedAttempt, RuntimeException cause, Duration delay) {}
    }

    /// Where a bound flow will run, relative to transactions owned by its caller.
    enum Placement {
        /// The flow owns its transactions: nothing is open when it starts. The default.
        STANDALONE,

        /// The flow runs inside a transaction owned by its caller, e.g. a BPMN engine running a `JavaDelegate` as part
        /// of a job. The whole flow is treated as being inside a boundary, so retries and fork/join are
        /// rejected when
        /// binding: the caller retries the whole unit (BPMN job retries) and the flow must stay on the caller's
        /// thread.
        PARTICIPATING
    }

    /// When and how often a [Definition.Retry] retries.
    ///
    /// There is deliberately no "retry everything" default: retrying a failed business rule (insufficient funds,
    /// validation) or a non-idempotent step is how double postings happen. Name the failures that are safe to
    /// retry, typically optimistic-locking conflicts.
    ///
    /// @param maxAttempts total number of attempts, including the first one; at least 1
    /// @param retryable which failures are retried; everything else is rethrown immediately
    /// @param initialBackoff delay after the first failed attempt; [Duration#ZERO] for no backoff
    /// @param multiplier growth factor of the delay per failed attempt; at least 1
    /// @param maxBackoff upper bound for the delay
    /// @param jitter fraction in `[0, 1]` by which each delay is randomly shortened
    record RetryPolicy(
            int maxAttempts,
            Predicate<? super RuntimeException> retryable,
            Duration initialBackoff,
            double multiplier,
            Duration maxBackoff,
            double jitter) {

        public RetryPolicy {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            requireNonNull(retryable, "retryable");
            requireNonNull(initialBackoff, "initialBackoff");
            requireNonNull(maxBackoff, "maxBackoff");
            if (initialBackoff.isNegative() || maxBackoff.compareTo(initialBackoff) < 0) {
                throw new IllegalArgumentException("backoff must satisfy 0 <= initialBackoff <= maxBackoff");
            }
            if (multiplier < 1.0) {
                throw new IllegalArgumentException("multiplier must be >= 1");
            }
            if (jitter < 0.0 || jitter > 1.0) {
                throw new IllegalArgumentException("jitter must be within [0, 1]");
            }
        }

        /// Retries failures matching `retryable` immediately, up to `maxAttempts` attempts in total.
        public static RetryPolicy on(int maxAttempts, Predicate<? super RuntimeException> retryable) {
            return new RetryPolicy(maxAttempts, retryable, Duration.ZERO, 1.0, Duration.ZERO, 0.0);
        }

        /// Exponential backoff: `initial`, `initial * multiplier`, ... capped at `max`.
        public RetryPolicy withBackoff(Duration initial, double multiplier, Duration max) {
            return new RetryPolicy(maxAttempts, retryable, initial, multiplier, max, jitter);
        }

        public RetryPolicy withJitter(double jitter) {
            return new RetryPolicy(maxAttempts, retryable, initialBackoff, multiplier, maxBackoff, jitter);
        }

        /// The delay before the attempt following `failedAttempt` (1-based).
        public Duration delayAfter(int failedAttempt, RandomGenerator random) {
            if (initialBackoff.isZero()) {
                return Duration.ZERO;
            }
            double base = initialBackoff.toNanos() * Math.pow(multiplier, failedAttempt - 1.0);
            double capped = Math.min(base, maxBackoff.toNanos());
            double factor = 1.0 - jitter * random.nextDouble();
            return Duration.ofNanos((long) (capped * factor));
        }
    }

    /// A [RetryPolicy], referenced by name (resolved by the [Engine], e.g. from configuration properties)
    /// or given
    /// directly.
    sealed interface RetryRef {
        /// Optimistic-locking and similar concurrency conflicts.
        RetryRef CONFLICT = new ByName("conflict");

        static RetryRef of(String name) {
            return new ByName(name);
        }

        static RetryRef of(RetryPolicy policy) {
            return new Direct(policy);
        }

        record ByName(String name) implements RetryRef {
            public ByName {
                requireNonNull(name, "name");
            }

            @Override
            @NonNull public String toString() {
                return name;
            }
        }

        record Direct(RetryPolicy policy) implements RetryRef {
            public Direct {
                requireNonNull(policy, "policy");
            }

            @Override
            @NonNull public String toString() {
                return "direct";
            }
        }
    }

    /// Pauses between retry attempts. Replaceable so that tests do not have to wait.
    @FunctionalInterface
    interface Sleeper {
        Sleeper THREAD = Thread::sleep;

        void sleep(Duration duration) throws InterruptedException;
    }

    /// Describes the node an interceptor is wrapping.
    ///
    /// @param name the step's usecase name or the name given with [Definition#named]
    /// @param kind `"step"`, `"check"`, `"named"` or `"retry"`
    /// @param path the position in the flow
    record Node(String name, String kind, String path) {
        public Node {
            requireNonNull(name, "name");
            requireNonNull(kind, "kind");
            requireNonNull(path, "path");
        }
    }

    /// The flow DSL. Use with `import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.*`.
    ///
    /// Every method only builds a [Definition]; nothing runs until a [Engine] binds it. Definitions can
    /// therefore be static constants:
    ///
    /// ```java
    /// public static final Definition<TransferCtx> TRANSFER = sequence(
    ///         check(TransferCtx::validation, ValidateTransfer.class),
    ///         step(TransferCtx::posting, PostJournalEntry.class, TransferCtx::withEntry)
    ///                 .within(BoundaryRef.REQUIRED)
    ///                 .retryOn(RetryRef.CONFLICT))
    ///         .named("transfer");
    /// ```
    final class Dsl {
        private Dsl() {}

        // --- basics ---

        public static <C extends MifosFlowContext> Definition<C> noop() {
            return new Definition.Noop<>();
        }

        public static <C extends MifosFlowContext> Definition<C> act(Function<? super C, ? extends C> fn) {
            return new Definition.Act<>(fn);
        }

        public static <C extends MifosFlowContext> Definition<C> tap(Consumer<? super C> action) {
            return new Definition.Tap<>(action);
        }

        public static <C extends MifosFlowContext> Definition<C> named(String name, Definition<C> body) {
            return new Definition.Named<>(name, body);
        }

        @SafeVarargs
        @SuppressWarnings("varargs")
        public static <C extends MifosFlowContext> Definition<C> sequence(Definition<C>... steps) {
            return new Definition.Sequence<>(List.of(steps));
        }

        public static <C extends MifosFlowContext> Definition<C> sequence(List<Definition<C>> steps) {
            return new Definition.Sequence<>(steps);
        }

        // --- usecases ---

        /// A step with a usecase instance.
        public static <C extends MifosFlowContext, REQ extends MifosRequest, RES extends MifosResponse>
                Definition<C> step(
                        Function<? super C, ? extends REQ> request,
                        MifosUsecase<? super REQ, ? extends RES> usecase,
                        BiFunction<? super C, ? super RES, ? extends C> merge) {
            return new Definition.Step<>(request, new UsecaseRef.Direct<>(usecase), merge);
        }

        /// A step with a usecase resolved by type when the flow is bound (in Spring: the bean of that type).
        public static <
                        C extends MifosFlowContext,
                        REQ extends MifosUsecase.MifosRequest,
                        RES extends MifosUsecase.MifosResponse,
                        U extends MifosUsecase<REQ, RES>>
                Definition<C> step(
                        Function<? super C, ? extends REQ> request,
                        Class<U> usecase,
                        BiFunction<? super C, ? super RES, ? extends C> merge) {
            return new Definition.Step<>(request, new UsecaseRef.ByType<REQ, RES>(usecase), merge);
        }

        public static <
                        C extends MifosFlowContext,
                        REQ extends MifosUsecase.MifosRequest,
                        RES extends MifosUsecase.MifosResponse>
                Definition<C> check(
                        Function<? super C, ? extends REQ> request, MifosUsecase<? super REQ, ? extends RES> usecase) {
            return new Definition.Check<>(request, new UsecaseRef.Direct<REQ, RES>(usecase));
        }

        public static <
                        C extends MifosFlowContext,
                        REQ extends MifosUsecase.MifosRequest,
                        RES extends MifosUsecase.MifosResponse,
                        U extends MifosUsecase<REQ, RES>>
                Definition<C> check(Function<? super C, ? extends REQ> request, Class<U> usecase) {
            return new Definition.Check<>(request, new UsecaseRef.ByType<REQ, RES>(usecase));
        }

        public static <C extends MifosFlowContext, D extends MifosFlowContext> Definition<C> subflow(
                Function<? super C, ? extends D> enter,
                Definition<D> body,
                BiFunction<? super C, ? super D, ? extends C> exit) {
            return new Definition.Subflow<>(enter, body, exit);
        }

        // --- decision and iteration ---

        public static <C extends MifosFlowContext> Definition<C> when(
                Predicate<? super C> condition, Definition<C> then, Definition<C> otherwise) {
            return new Definition.When<>(condition, then, otherwise);
        }

        public static <C extends MifosFlowContext> Definition<C> when(
                Predicate<? super C> condition, Definition<C> then) {
            return when(condition, then, noop());
        }

        public static <C extends MifosFlowContext> Definition<C> repeatWhile(
                Predicate<? super C> condition, int maxIterations, Definition<C> body) {
            return new Definition.RepeatWhile<>(condition, maxIterations, body);
        }

        /// Folds every item into the context.
        public static <C extends MifosFlowContext, I> Definition<C> forEach(
                Function<? super C, ? extends Iterable<? extends I>> items,
                BiFunction<? super C, ? super I, ? extends C> fold) {
            return new Definition.ForEach<>(items, fold, noop());
        }

        /// Runs `body` once per item, after `enter` has put the item into the context.
        public static <C extends MifosFlowContext, I> Definition<C> forEach(
                Function<? super C, ? extends Iterable<? extends I>> items,
                BiFunction<? super C, ? super I, ? extends C> enter,
                Definition<C> body) {
            return new Definition.ForEach<>(items, enter, body);
        }

        // --- parallelism ---

        /// Fork/join on the [ExecutorRef#DEFAULT] executor.
        public static <C extends MifosFlowContext> Definition<C> forkJoin(
                Function<? super C, ? extends C> snapshot, List<Definition<C>> forks, BinaryOperator<C> merge) {
            return forkJoin(snapshot, forks, merge, ExecutorRef.DEFAULT);
        }

        public static <C extends MifosFlowContext> Definition<C> forkJoin(
                Function<? super C, ? extends C> snapshot,
                List<Definition<C>> forks,
                BinaryOperator<C> merge,
                ExecutorRef executor) {
            return new Definition.ForkJoin<>(snapshot, forks, merge, executor);
        }

        // --- boundaries, retries, failures ---

        public static <C extends MifosFlowContext> Definition<C> within(BoundaryRef boundary, Definition<C> body) {
            return body.within(boundary);
        }

        public static <C extends MifosFlowContext> Definition<C> retryOn(RetryRef policy, Definition<C> body) {
            return body.retryOn(policy);
        }

        public static <C extends MifosFlowContext> Definition<C> retryOn(RetryPolicy policy, Definition<C> body) {
            return body.retryOn(policy);
        }

        /// Every attempt runs in a fresh boundary: `body.within(boundary).retryOn(policy)`.
        public static <C extends MifosFlowContext> Definition<C> transactional(
                BoundaryRef boundary, RetryRef policy, Definition<C> body) {
            return body.within(boundary).retryOn(policy);
        }

        public static <C extends MifosFlowContext> Definition<C> recover(
                Definition<C> body,
                Predicate<? super RuntimeException> when,
                BiFunction<? super C, ? super RuntimeException, ? extends C> handler) {
            return body.recover(when, handler);
        }

        public static <C extends MifosFlowContext> Definition.Compensable<C> compensable(
                Definition<C> action, Definition<C> undo) {
            return new Definition.Compensable<>(action, undo);
        }

        @SafeVarargs
        @SuppressWarnings("varargs")
        public static <C extends MifosFlowContext> Definition<C> saga(Definition.Compensable<C>... steps) {
            return new Definition.Saga<>(List.of(steps));
        }

        public static <C extends MifosFlowContext> Definition<C> saga(List<Definition.Compensable<C>> steps) {
            return new Definition.Saga<>(steps);
        }
    }
}
