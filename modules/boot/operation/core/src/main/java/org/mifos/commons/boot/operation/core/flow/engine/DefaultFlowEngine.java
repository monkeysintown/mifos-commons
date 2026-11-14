///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.flow.engine;

import static java.util.Objects.requireNonNull;
import static org.mifos.commons.boot.core.model.MifosErrorCategory.INTERNAL;
import static org.mifos.commons.boot.operation.core.flow.exception.MifosFlowException.MifosFlowErrorCode.MIFOS_COMMONS_ERROR_FLOW_DEFINITION;
import static org.mifos.commons.boot.operation.core.flow.exception.MifosFlowException.MifosFlowErrorCode.MIFOS_COMMONS_ERROR_FLOW_ITERATION_LIMIT;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.operation.core.flow.MifosFlow;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Act;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Check;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.ForEach;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.ForkJoin;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Named;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Noop;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Recover;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.RepeatWhile;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Retry;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Saga;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Sequence;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Step;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Subflow;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Tap;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.When;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Within;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.MifosFlowContext;
import org.mifos.commons.boot.operation.core.flow.exception.MifosFlowException;
import org.mifos.commons.boot.operation.core.flow.spi.Boundary;
import org.mifos.commons.boot.operation.core.flow.spi.BoundaryResolver;
import org.mifos.commons.boot.operation.core.flow.spi.ExecutorResolver;
import org.mifos.commons.boot.operation.core.flow.spi.RetryPolicyResolver;
import org.mifos.commons.boot.operation.core.flow.spi.UsecaseRef;
import org.mifos.commons.boot.operation.core.flow.spi.UsecaseResolver;
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase;
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase.MifosRequest;

/// The standard [MifosFlow.Engine]: compiles a definition into nested closures, once, at bind time.
///
/// Structural rules enforced when binding:
///
/// - no retry inside a boundary (the transaction would already be rollback-only when retrying);
/// - no fork/join inside a boundary (forks run on other threads and would escape the transaction).
///
/// Both rules are decided from the [MifosFlow.BoundaryRef] alone, never from how it resolves, so a definition that is
/// invalid
/// in production is also invalid in a test that resolves every boundary to [Boundary#NONE].
public final class DefaultFlowEngine implements MifosFlow.Engine {
    private final Map<String, Boundary> boundaries;
    private final BoundaryResolver boundaryResolver;
    private final Map<String, MifosFlow.RetryPolicy> retryPolicies;
    private final RetryPolicyResolver retryPolicyResolver;
    private final Map<String, Executor> executors;
    private final ExecutorResolver executorResolver;
    private final Map<Class<?>, Object> usecases;
    private final UsecaseResolver usecaseResolver;
    private final List<MifosFlow.Interceptor> interceptors;
    private final MifosFlow.Sleeper sleeper;

    private DefaultFlowEngine(Builder builder) {
        this.boundaries = Map.copyOf(builder.boundaries);
        this.boundaryResolver = builder.boundaryResolver;
        this.retryPolicies = Map.copyOf(builder.retryPolicies);
        this.retryPolicyResolver = builder.retryPolicyResolver;
        this.executors = Map.copyOf(builder.executors);
        this.executorResolver = builder.executorResolver;
        this.usecases = Map.copyOf(builder.usecases);
        this.usecaseResolver = builder.usecaseResolver;
        this.interceptors = List.copyOf(builder.interceptors);
        this.sleeper = builder.sleeper;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <C extends MifosFlowContext> MifosFlow<C> bind(
            MifosFlow.Definition<C> definition, MifosFlow.Placement placement) {
        requireNonNull(definition, "definition");
        requireNonNull(placement, "placement");

        var root = switch (placement) {
            case STANDALONE -> Scope.ROOT;
            case PARTICIPATING -> Scope.ROOT.intoBoundary("the caller's transaction (placement PARTICIPATING)");
        };

        return compile(definition, root);
    }

    // --- compilation ---

    private <C extends MifosFlowContext> MifosFlow<C> compile(MifosFlow.Definition<C> definition, Scope scope) {
        return switch (definition) {
            case Noop<C> noop -> ctx -> ctx;
            case Act<C> act -> {
                var fn = act.fn();
                yield fn::apply;
            }
            case Tap<C> tap -> {
                var action = tap.action();
                yield ctx -> {
                    action.accept(ctx);
                    return ctx;
                };
            }
            case Step<C, ?, ?> step -> compileStep(step, scope);
            case Check<C, ?> check -> compileCheck(check, scope);
            case Sequence<C> sequence -> compileSequence(sequence, scope);
            case When<C> when -> {
                var condition = when.condition();
                MifosFlow<C> then = compile(when.then(), scope.enter(Scope.WHEN_THEN));
                MifosFlow<C> otherwise = compile(when.otherwise(), scope.enter(Scope.WHEN_ELSE));
                yield ctx -> condition.test(ctx) ? then.run(ctx) : otherwise.run(ctx);
            }
            case RepeatWhile<C> repeat -> compileRepeatWhile(repeat, scope);
            case ForEach<C, ?> forEach -> compileForEach(forEach, scope);
            case Subflow<C, ?> subflow -> compileSubflow(subflow, scope);
            case ForkJoin<C> forkJoin -> compileForkJoin(forkJoin, scope);
            case Within<C> within -> compileWithin(within, scope);
            case Retry<C> retry -> compileRetry(retry, scope);
            case Recover<C> recover -> compileRecover(recover, scope);
            case Saga<C> saga -> compileSaga(saga, scope);
            case Named<C> named -> {
                var inner = scope.enter(named.name());
                yield intercepted(
                        new MifosFlow.Node(named.name(), "named", inner.path()), compile(named.body(), inner));
            }
        };
    }

    private <C extends MifosFlowContext, REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse>
            MifosFlow<C> compileStep(Step<C, REQ, RES> step, Scope scope) {
        var inner = scope.enter(step.usecase().name());
        var usecase = resolveUsecase(step.usecase(), inner);
        var request = step.request();
        var merge = step.merge();

        MifosFlow<C> flow = ctx -> merge.apply(ctx, usecase.execute(request.apply(ctx)));

        return intercepted(new MifosFlow.Node(step.usecase().name(), "step", inner.path()), flow);
    }

    private <C extends MifosFlowContext, REQ extends MifosRequest> MifosFlow<C> compileCheck(
            Check<C, REQ> check, Scope scope) {
        var inner = scope.enter(check.usecase().name());
        var usecase = resolveUsecase(check.usecase(), inner);
        var request = check.request();

        MifosFlow<C> flow = ctx -> {
            usecase.execute(request.apply(ctx));
            return ctx;
        };

        return intercepted(new MifosFlow.Node(check.usecase().name(), "check", inner.path()), flow);
    }

    private <C extends MifosFlowContext> MifosFlow<C> compileSequence(Sequence<C> sequence, Scope scope) {
        var steps = new ArrayList<MifosFlow<C>>(sequence.steps().size());

        for (MifosFlow.Definition<C> step : sequence.steps()) {
            steps.add(compile(step, scope));
        }

        return ctx -> {
            C current = ctx;

            for (MifosFlow<C> step : steps) {
                current = step.run(current);
            }

            return current;
        };
    }

    private <C extends MifosFlowContext> MifosFlow<C> compileRepeatWhile(RepeatWhile<C> repeat, Scope scope) {
        var inner = scope.enter("repeatWhile");
        var condition = repeat.condition();
        int max = repeat.maxIterations();
        var body = compile(repeat.body(), inner);

        return ctx -> {
            C current = ctx;
            int iterations = 0;

            while (condition.test(current)) {
                if (iterations++ == max) {
                    throw new MifosFlowException(MifosError.of(
                            INTERNAL, MIFOS_COMMONS_ERROR_FLOW_ITERATION_LIMIT, List.of(inner.path(), max)));
                }

                current = body.run(current);
            }

            return current;
        };
    }

    private <C extends MifosFlowContext, I> MifosFlow<C> compileForEach(ForEach<C, I> forEach, Scope scope) {
        var items = forEach.items();
        var enter = forEach.enter();
        var body = compile(forEach.body(), scope.enter("forEach"));

        return ctx -> {
            C current = ctx;

            for (I item : items.apply(ctx)) {
                current = body.run(enter.apply(current, item));
            }

            return current;
        };
    }

    private <C extends MifosFlowContext, D extends MifosFlowContext> MifosFlow<C> compileSubflow(
            Subflow<C, D> subflow, Scope scope) {
        var enter = subflow.enter();
        var exit = subflow.exit();
        var body = compile(subflow.body(), scope.enter("subflow"));

        return ctx -> exit.apply(ctx, body.run(enter.apply(ctx)));
    }

    private <C extends MifosFlowContext> MifosFlow<C> compileWithin(Within<C> within, Scope scope) {
        if (within.boundary().isNone()) {
            return compile(within.body(), scope);
        }

        var label = "within(" + within.boundary() + ")";
        var inner = scope.enter(label).intoBoundary(label);
        var boundary = resolveBoundary(within.boundary(), inner);
        var body = compile(within.body(), inner);

        return ctx -> boundary.run(() -> body.run(ctx));
    }

    private <C extends MifosFlowContext> MifosFlow<C> compileRetry(Retry<C> retry, Scope scope) {
        var inner = scope.enter("retry(" + retry.policy() + ")");

        if (scope.insideBoundary()) {
            throw new MifosFlowException(MifosError.of(
                    INTERNAL, MIFOS_COMMONS_ERROR_FLOW_DEFINITION, List.of(inner.path(), scope.boundary())));
        }

        var policy = resolveRetryPolicy(retry.policy(), inner);
        var body = compile(retry.body(), inner);
        var node = new MifosFlow.Node(retry.policy().toString(), "retry", inner.path());

        return ctx -> {
            for (int attempt = 1; ; attempt++) {
                try {
                    return body.run(ctx);
                } catch (RuntimeException e) {
                    if (attempt >= policy.maxAttempts() || !policy.retryable().test(e)) {
                        throw e;
                    }

                    Duration delay = policy.delayAfter(attempt, ThreadLocalRandom.current());

                    for (var interceptor : interceptors) {
                        interceptor.onRetry(node, attempt, e, delay);
                    }

                    pause(delay, e);
                }
            }
        };
    }

    private <C extends MifosFlowContext> MifosFlow<C> compileRecover(Recover<C> recover, Scope scope) {
        var when = recover.when();
        var handler = recover.handler();
        var body = compile(recover.body(), scope.enter("recover"));

        return ctx -> {
            try {
                return body.run(ctx);
            } catch (RuntimeException e) {
                if (!when.test(e)) {
                    throw e;
                }

                return handler.apply(ctx, e);
            }
        };
    }

    private record Undo<C extends MifosFlowContext>(MifosFlow<C> flow, C context) {}

    private <C extends MifosFlowContext> MifosFlow<C> compileSaga(Saga<C> saga, Scope scope) {
        var actions = new ArrayList<MifosFlow<C>>();
        var undos = new ArrayList<MifosFlow<C>>();

        for (int i = 0; i < saga.steps().size(); i++) {
            var step = saga.steps().get(i);
            actions.add(compile(step.action(), scope.enter("saga[" + i + "].action")));
            undos.add(compile(step.undo(), scope.enter("saga[" + i + "].undo")));
        }

        return ctx -> {
            var completed = new ArrayDeque<Undo<C>>();
            var current = ctx;

            for (int i = 0; i < actions.size(); i++) {
                try {
                    current = actions.get(i).run(current);
                } catch (RuntimeException failure) {
                    while (!completed.isEmpty()) {
                        var undo = completed.pop();

                        try {
                            undo.flow().run(undo.context());
                        } catch (RuntimeException undoFailure) {
                            failure.addSuppressed(undoFailure);
                        }
                    }

                    throw failure;
                }

                completed.push(new Undo<>(undos.get(i), current));
            }

            return current;
        };
    }

    private record Indexed<T>(int index, T value) {}

    private <C extends MifosFlowContext> MifosFlow<C> compileForkJoin(
            MifosFlow.Definition.ForkJoin<C> forkJoin, Scope scope) {
        var inner = scope.enter(Scope.FORK_JOIN);

        if (scope.insideBoundary()) {
            throw new MifosFlowException(MifosError.of(
                    INTERNAL, MIFOS_COMMONS_ERROR_FLOW_DEFINITION, List.of(inner.path(), scope.boundary())));
        }

        var executor = resolveExecutor(forkJoin.executor(), inner);
        var snapshot = forkJoin.snapshot();
        var merge = forkJoin.merge();
        var forks = new ArrayList<MifosFlow<C>>();

        for (int i = 0; i < forkJoin.forks().size(); i++) {
            forks.add(compile(forkJoin.forks().get(i), inner.enter(Scope.FORK.formatted(i))));
        }

        return ctx -> {
            if (forks.isEmpty()) {
                return ctx;
            }

            var completion = new ExecutorCompletionService<Indexed<C>>(executor);
            var futures = new ArrayList<Future<Indexed<C>>>(forks.size());
            var results = new ArrayList<C>(Collections.nCopies(forks.size(), null));

            try {
                for (int i = 0; i < forks.size(); i++) {
                    int index = i;
                    var fork = forks.get(i);
                    var input = snapshot.apply(ctx);

                    futures.add(completion.submit(() -> new Indexed<>(index, fork.run(input))));
                }

                for (int done = 0; done < forks.size(); done++) {
                    Indexed<C> result = completion.take().get();
                    results.set(result.index(), result.value());
                }
            } catch (ExecutionException e) {
                futures.forEach(f -> f.cancel(true));
                throw unchecked(e.getCause());
            } catch (InterruptedException e) {
                futures.forEach(f -> f.cancel(true));
                Thread.currentThread().interrupt();
                var cancelled = new CancellationException("interrupted while waiting for forks");
                cancelled.initCause(e);
                throw cancelled;
            } catch (RuntimeException e) { // e.g. RejectedExecutionException while submitting
                futures.forEach(f -> f.cancel(true));
                throw e;
            }

            var merged = ctx;

            for (var result : results) {
                merged = merge.apply(merged, result);
            }

            return merged;
        };
    }

    private <C extends MifosFlowContext> MifosFlow<C> intercepted(MifosFlow.Node node, MifosFlow<C> flow) {
        var result = flow;

        for (var interceptor : interceptors.reversed()) {
            final var next = result;
            result = ctx -> interceptor.intercept(node, ctx, next);
        }

        return result;
    }

    // --- resolution ---

    private <REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse>
            MifosUsecase<? super REQ, ? extends RES> resolveUsecase(UsecaseRef<REQ, RES> ref, Scope scope) {
        return switch (ref) {
            case UsecaseRef.Direct<REQ, RES> direct -> direct.usecase();
            case UsecaseRef.ByType<REQ, RES> byType -> lookup(byType.type(), scope);
        };
    }

    private <U> U lookup(Class<U> type, Scope scope) {
        var registered = usecases.get(type);

        if (registered != null) {
            return type.cast(registered);
        }

        return usecaseResolver
                .resolve(type)
                .orElseThrow(() -> new MifosFlowException(MifosError.of(
                        INTERNAL, MIFOS_COMMONS_ERROR_FLOW_DEFINITION, List.of(scope.path(), type.getName()))));
    }

    private Boundary resolveBoundary(MifosFlow.BoundaryRef ref, Scope scope) {
        return switch (ref) {
            case MifosFlow.BoundaryRef.Direct direct -> direct.boundary();
            case MifosFlow.BoundaryRef.ByName byName ->
                resolve(byName.name(), boundaries, boundaryResolver::resolve, "boundary", scope);
        };
    }

    private MifosFlow.RetryPolicy resolveRetryPolicy(MifosFlow.RetryRef ref, Scope scope) {
        return switch (ref) {
            case MifosFlow.RetryRef.Direct direct -> direct.policy();
            case MifosFlow.RetryRef.ByName byName ->
                resolve(byName.name(), retryPolicies, retryPolicyResolver::resolve, "retry policy", scope);
        };
    }

    private Executor resolveExecutor(MifosFlow.ExecutorRef ref, Scope scope) {
        return switch (ref) {
            case MifosFlow.ExecutorRef.Direct direct -> direct.executor();
            case MifosFlow.ExecutorRef.ByName byName ->
                resolve(byName.name(), executors, executorResolver::resolve, "executor", scope);
        };
    }

    private static <T> T resolve(
            String name,
            Map<String, T> registered,
            java.util.function.Function<String, Optional<T>> fallback,
            String what,
            Scope scope) {
        var value = registered.get(name);

        if (value != null) {
            return value;
        }

        return fallback.apply(name)
                .orElseThrow(() -> new MifosFlowException(MifosError.of(
                        INTERNAL,
                        MIFOS_COMMONS_ERROR_FLOW_DEFINITION,
                        List.of(scope.path(), scope.path(), "no " + what + " named '" + name + "'"))));
    }

    // --- runtime helpers ---

    private void pause(Duration delay, RuntimeException pending) {
        if (delay.isZero()) {
            return;
        }

        try {
            sleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pending.addSuppressed(e);
            throw pending;
        }
    }

    private static RuntimeException unchecked(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(cause);
    }

    /// Compile-time position in the definition tree.
    ///
    /// @param boundary the innermost enclosing boundary, for error messages; `null` outside any boundary
    private record Scope(String path, String boundary) {
        static final Scope ROOT = new Scope("", null);
        static final String WHEN_THEN = "when.then";
        static final String WHEN_ELSE = "when.else";
        static final String FORK_JOIN = "fork.join";
        static final String FORK = "fork[%d]";

        Scope enter(String segment) {
            return new Scope(path.isEmpty() ? segment : path + " > " + segment, boundary);
        }

        Scope intoBoundary(String label) {
            return new Scope(path, label);
        }

        boolean insideBoundary() {
            return boundary != null;
        }
    }

    // --- builder ---

    /// Builds a [DefaultFlowEngine]. Lookups try the explicitly registered entries first, then the resolver.
    /// Anything that cannot be resolved makes [#bind] fail.
    public static final class Builder {
        private final Map<String, Boundary> boundaries = new HashMap<>();
        private BoundaryResolver boundaryResolver = name -> Optional.empty();
        private final Map<String, MifosFlow.RetryPolicy> retryPolicies = new HashMap<>();
        private RetryPolicyResolver retryPolicyResolver = name -> Optional.empty();
        private final Map<String, Executor> executors = new HashMap<>();
        private ExecutorResolver executorResolver = name -> Optional.empty();
        private final Map<Class<?>, Object> usecases = new HashMap<>();
        private UsecaseResolver usecaseResolver = new UsecaseResolver() {
            @Override
            public <U> Optional<U> resolve(Class<U> type) {
                return Optional.empty();
            }
        };
        private final List<MifosFlow.Interceptor> interceptors = new ArrayList<>();
        private MifosFlow.Sleeper sleeper = MifosFlow.Sleeper.THREAD;

        private Builder() {}

        public Builder boundary(String name, Boundary boundary) {
            boundaries.put(requireNonNull(name), requireNonNull(boundary));
            return this;
        }

        public Builder boundary(MifosFlow.BoundaryRef.ByName ref, Boundary boundary) {
            return boundary(ref.name(), boundary);
        }

        public Builder boundaries(BoundaryResolver resolver) {
            this.boundaryResolver = requireNonNull(resolver);
            return this;
        }

        /// Resolves every named boundary to [Boundary#NONE], for tests. Validation is unaffected.
        public Builder withoutBoundaries() {
            return boundaries(name -> Optional.of(Boundary.NONE));
        }

        public Builder retryPolicy(String name, MifosFlow.RetryPolicy policy) {
            retryPolicies.put(requireNonNull(name), requireNonNull(policy));
            return this;
        }

        public Builder retryPolicy(MifosFlow.RetryRef.ByName ref, MifosFlow.RetryPolicy policy) {
            return retryPolicy(ref.name(), policy);
        }

        public Builder retryPolicies(RetryPolicyResolver resolver) {
            this.retryPolicyResolver = requireNonNull(resolver);
            return this;
        }

        public Builder executor(String name, Executor executor) {
            executors.put(requireNonNull(name), requireNonNull(executor));
            return this;
        }

        public Builder executor(MifosFlow.ExecutorRef.ByName ref, Executor executor) {
            return executor(ref.name(), executor);
        }

        public Builder executors(ExecutorResolver resolver) {
            this.executorResolver = requireNonNull(resolver);
            return this;
        }

        public <U extends MifosUsecase<?, ?>> Builder usecase(Class<U> type, U usecase) {
            usecases.put(requireNonNull(type), requireNonNull(usecase));
            return this;
        }

        public Builder usecases(UsecaseResolver resolver) {
            this.usecaseResolver = requireNonNull(resolver);
            return this;
        }

        /// Adds an interceptor; the first one added is the outermost.
        public Builder interceptor(MifosFlow.Interceptor interceptor) {
            interceptors.add(requireNonNull(interceptor));
            return this;
        }

        public Builder sleeper(MifosFlow.Sleeper sleeper) {
            this.sleeper = requireNonNull(sleeper);
            return this;
        }

        public DefaultFlowEngine build() {
            return new DefaultFlowEngine(this);
        }
    }
}
