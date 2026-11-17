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
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase;
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase.MifosRequest;
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase.MifosResponse;
import org.mifos.commons.boot.operation.core.usecase.UsecaseRef;
import org.mifos.commons.boot.operation.flow.exception.MifosFlowException;
import org.mifos.commons.boot.operation.flow.spi.BoundaryRef;

/// An executable flow: a function from one immutable context to the next.
///
/// Flows are not written by implementing this interface. They are described with the [Dsl] as a [Definition] and
/// turned into a `MifosFlow` by an [Engine], which supplies transactions, executors, retry policies, usecases and
/// interceptors.
///
/// @param <C> the context type
@FunctionalInterface
public interface MifosFlow<C extends MifosFlow.MifosFlowContext> {
    /// Runs the flow.
    ///
    /// @param context the context to start with
    /// @return the resulting context
    C run(C context);

    /// Marker interface for context objects.
    interface MifosFlowContext extends Serializable {}

    /// The description of a flow: what runs, in which order, under which conditions and boundaries.
    ///
    /// Definitions are immutable data and do not run anything. An [Engine] validates and compiles them into an
    /// executable [MifosFlow], resolving the usecases, boundaries, executors and retry policies they reference.
    /// Build them with the static methods in [Dsl] and the fluent methods below:
    ///
    /// ```java
    /// step(TransferCtx::posting, PostJournalEntry.class, TransferCtx::withEntry)
    ///         .within(BoundaryRef.REQUIRED)
    ///         .retryOn(RetryRef.CONFLICT)
    /// ```
    ///
    /// Each fluent method wraps everything before it, so the code reads in the order of nesting: the retry above
    /// wraps the transaction, which wraps the step.
    ///
    /// @param <C> the context type
    sealed interface Definition<C extends MifosFlowContext> {

        // --- fluent composition ---

        /// Runs this, then `next`.
        ///
        /// @param next the part to run afterwards
        /// @return a sequence of this and `next`
        default Definition<C> andThen(Definition<C> next) {
            return new Sequence<>(List.of(this, next));
        }

        /// Names this part of the flow; [Interceptor]s see the name and error messages include it.
        ///
        /// @param name the name
        /// @return this part, named
        default Definition<C> named(String name) {
            return new Named<>(name, this);
        }

        /// Runs this inside a unit-of-work boundary, typically a transaction.
        ///
        /// @param boundary the boundary
        /// @return this part, wrapped in the boundary
        default Definition<C> within(BoundaryRef boundary) {
            return new Within<>(boundary, this);
        }

        /// Retries this according to the referenced policy. Must not be placed inside a boundary.
        ///
        /// @param policy the retry policy reference
        /// @return this part, wrapped in the retry
        default Definition<C> retryOn(RetryRef policy) {
            return new Retry<>(policy, this);
        }

        /// Retries this according to the given policy. Must not be placed inside a boundary.
        ///
        /// @param policy the retry policy
        /// @return this part, wrapped in the retry
        default Definition<C> retryOn(RetryPolicy policy) {
            return retryOn(RetryRef.of(policy));
        }

        /// Turns failures matching `when` into a context; the handler receives the context this part was started with.
        ///
        /// @param when which failures to recover from; all others are rethrown
        /// @param handler computes the resulting context from the original context and the failure
        /// @return this part, wrapped in the recovery
        default Definition<C> recover(
                Predicate<? super RuntimeException> when,
                BiFunction<? super C, ? super RuntimeException, ? extends C> handler) {
            return new Recover<>(this, when, handler);
        }

        // --- nodes ---

        /// Does nothing and returns the context unchanged.
        ///
        /// @param <C> the context type
        record Noop<C extends MifosFlowContext>() implements Definition<C> {}

        /// An ad-hoc transformation of the context. Prefer [Definition.Step] for anything that is a business rule.
        ///
        /// @param <C> the context type
        /// @param fn the transformation
        record Act<C extends MifosFlowContext>(Function<? super C, ? extends C> fn) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param fn the transformation; not `null`
            public Act {
                requireNonNull(fn, "fn");
            }
        }

        /// A side effect (e.g. publishing an event) that does not change the context.
        ///
        /// @param <C> the context type
        /// @param action the side effect
        record Tap<C extends MifosFlowContext>(Consumer<? super C> action) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param action the side effect; not `null`
            public Tap {
                requireNonNull(action, "action");
            }
        }

        /// Derives a request from the context, executes the usecase and merges the response into a new context.
        ///
        /// @param <C> the context type
        /// @param <REQ> the usecase request type
        /// @param <RES> the usecase response type
        /// @param request derives the request from the context
        /// @param usecase the usecase to execute
        /// @param merge merges the response into a new context
        record Step<C extends MifosFlowContext, REQ extends MifosRequest, RES extends MifosResponse>(
                Function<? super C, ? extends REQ> request,
                UsecaseRef<REQ, RES> usecase,
                BiFunction<? super C, ? super RES, ? extends C> merge)
                implements Definition<C> {
            /// Creates the node.
            ///
            /// @param request derives the request from the context; not `null`
            /// @param usecase the usecase to execute; not `null`
            /// @param merge merges the response into a new context; not `null`
            public Step {
                requireNonNull(request, "request");
                requireNonNull(usecase, "usecase");
                requireNonNull(merge, "merge");
            }
        }

        /// Executes a verifying usecase (one that throws on violation) and leaves the context unchanged.
        ///
        /// @param <C> the context type
        /// @param <REQ> the usecase request type
        /// @param request derives the request from the context
        /// @param usecase the verifying usecase; its response is ignored
        record Check<C extends MifosFlowContext, REQ extends MifosRequest>(
                Function<? super C, ? extends REQ> request, UsecaseRef<REQ, ?> usecase) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param request derives the request from the context; not `null`
            /// @param usecase the verifying usecase; not `null`
            public Check {
                requireNonNull(request, "request");
                requireNonNull(usecase, "usecase");
            }
        }

        /// Runs the steps in order, each one on the context returned by the previous one.
        ///
        /// @param <C> the context type
        /// @param steps the steps, copied into an immutable list
        @SuppressWarnings("Var")
        record Sequence<C extends MifosFlowContext>(List<Definition<C>> steps) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param steps the steps; not `null`, without `null` elements
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

        /// Runs `then` if `condition` holds for the context, `otherwise` if not.
        ///
        /// @param <C> the context type
        /// @param condition the condition
        /// @param then the branch taken when the condition holds
        /// @param otherwise the branch taken when it does not
        record When<C extends MifosFlowContext>(
                Predicate<? super C> condition, Definition<C> then, Definition<C> otherwise) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param condition the condition; not `null`
            /// @param then the branch taken when the condition holds; not `null`
            /// @param otherwise the branch taken when it does not; not `null`
            public When {
                requireNonNull(condition, "condition");
                requireNonNull(then, "then");
                requireNonNull(otherwise, "otherwise");
            }
        }

        /// Runs `body` while `condition` holds, failing after `maxIterations` iterations instead of looping forever.
        ///
        /// @param <C> the context type
        /// @param condition checked before every iteration
        /// @param maxIterations the maximum number of iterations; at least 1
        /// @param body the loop body
        record RepeatWhile<C extends MifosFlowContext>(
                Predicate<? super C> condition, int maxIterations, Definition<C> body) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param condition checked before every iteration; not `null`
            /// @param maxIterations the maximum number of iterations; at least 1
            /// @param body the loop body; not `null`
            /// @throws IllegalArgumentException if `maxIterations` is less than 1
            public RepeatWhile {
                requireNonNull(condition, "condition");
                requireNonNull(body, "body");
                if (maxIterations < 1) {
                    throw new IllegalArgumentException("maxIterations must be >= 1");
                }
            }
        }

        /// For every item: `ctx = body(enter(ctx, item))`. With a [Definition.Noop] body this is a plain fold.
        ///
        /// @param <C> the context type
        /// @param <I> the item type
        /// @param items extracts the items from the context
        /// @param enter puts an item into the context
        /// @param body runs once per item
        record ForEach<C extends MifosFlowContext, I>(
                Function<? super C, ? extends Iterable<? extends I>> items,
                BiFunction<? super C, ? super I, ? extends C> enter,
                Definition<C> body)
                implements Definition<C> {
            /// Creates the node.
            ///
            /// @param items extracts the items from the context; not `null`
            /// @param enter puts an item into the context; not `null`
            /// @param body runs once per item; not `null`
            public ForEach {
                requireNonNull(items, "items");
                requireNonNull(enter, "enter");
                requireNonNull(body, "body");
            }
        }

        /// Runs a flow with a different context type: `enter` maps into it, `exit` merges back.
        ///
        /// @param <C> the context type
        /// @param <D> the context type of the subflow
        /// @param enter maps the context into the subflow's context
        /// @param body the subflow
        /// @param exit merges the subflow's result back into the context
        record Subflow<C extends MifosFlowContext, D extends MifosFlowContext>(
                Function<? super C, ? extends D> enter,
                Definition<D> body,
                BiFunction<? super C, ? super D, ? extends C> exit)
                implements Definition<C> {
            /// Creates the node.
            ///
            /// @param enter maps the context into the subflow's context; not `null`
            /// @param body the subflow; not `null`
            /// @param exit merges the subflow's result back into the context; not `null`
            public Subflow {
                requireNonNull(enter, "enter");
                requireNonNull(body, "body");
                requireNonNull(exit, "exit");
            }
        }

        /// Runs `forks` in parallel, each on its own `snapshot` of the context, and merges the results in declaration
        /// order. Must not be placed inside a boundary: forks run on other threads and would escape it.
        ///
        /// @param <C> the context type
        /// @param snapshot creates the context each fork starts with
        /// @param forks the parallel parts, copied into an immutable list
        /// @param merge combines two results
        /// @param executor the executor the forks run on
        @SuppressWarnings("Var")
        record ForkJoin<C extends MifosFlowContext>(
                Function<? super C, ? extends C> snapshot,
                List<Definition<C>> forks,
                BinaryOperator<C> merge,
                ExecutorRef executor)
                implements Definition<C> {
            /// Creates the node.
            ///
            /// @param snapshot creates the context each fork starts with; not `null`
            /// @param forks the parallel parts; not `null`, without `null` elements
            /// @param merge combines two results; not `null`
            /// @param executor the executor the forks run on; not `null`
            public ForkJoin {
                requireNonNull(snapshot, "snapshot");
                forks = List.copyOf(forks);
                requireNonNull(merge, "merge");
                requireNonNull(executor, "executor");
            }
        }

        /// Runs `body` inside a unit-of-work boundary, typically a transaction.
        ///
        /// @param <C> the context type
        /// @param boundary the boundary
        /// @param body the wrapped part
        record Within<C extends MifosFlowContext>(BoundaryRef boundary, Definition<C> body) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param boundary the boundary; not `null`
            /// @param body the wrapped part; not `null`
            public Within {
                requireNonNull(boundary, "boundary");
                requireNonNull(body, "body");
            }
        }

        /// Retries `body` according to `policy`. Must not be placed inside a boundary.
        ///
        /// @param <C> the context type
        /// @param policy the retry policy reference
        /// @param body the retried part
        record Retry<C extends MifosFlowContext>(RetryRef policy, Definition<C> body) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param policy the retry policy reference; not `null`
            /// @param body the retried part; not `null`
            public Retry {
                requireNonNull(policy, "policy");
                requireNonNull(body, "body");
            }
        }

        /// Turns failures of `body` matching `when` into a context computed by `handler`.
        ///
        /// Recovering inside a boundary does not undo a rollback-only mark the failure may have left on the
        /// transaction; recover outside the boundary when the failure came from the database.
        ///
        /// @param <C> the context type
        /// @param body the guarded part
        /// @param when which failures to recover from; all others are rethrown
        /// @param handler computes the resulting context from the context `body` was started with and the
        /// failure
        record Recover<C extends MifosFlowContext>(
                Definition<C> body,
                Predicate<? super RuntimeException> when,
                BiFunction<? super C, ? super RuntimeException, ? extends C> handler)
                implements Definition<C> {
            /// Creates the node.
            ///
            /// @param body the guarded part; not `null`
            /// @param when which failures to recover from; not `null`
            /// @param handler computes the resulting context; not `null`
            public Recover {
                requireNonNull(body, "body");
                requireNonNull(when, "when");
                requireNonNull(handler, "handler");
            }
        }

        /// Runs the actions in order; if one fails, the undo steps of the completed actions run in reverse order.
        ///
        /// @param <C> the context type
        /// @param steps the actions with their undo steps, copied into an immutable list
        @SuppressWarnings("Var")
        record Saga<C extends MifosFlowContext>(List<Definition.Compensable<C>> steps) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param steps the actions with their undo steps; not `null`, without `null` elements
            public Saga {
                steps = List.copyOf(steps);
            }
        }

        /// An action paired with the step that undoes it. Not a flow on its own; see [Saga].
        ///
        /// @param <C> the context type
        /// @param action the action
        /// @param undo undoes the action; runs on the context the action returned
        record Compensable<C extends MifosFlowContext>(Definition<C> action, Definition<C> undo) {
            /// Creates the pair.
            ///
            /// @param action the action; not `null`
            /// @param undo undoes the action; not `null`
            public Compensable {
                requireNonNull(action, "action");
                requireNonNull(undo, "undo");
            }
        }

        /// A named part of the flow; [Interceptor]s see the name and error messages include it.
        ///
        /// @param <C> the context type
        /// @param name the name
        /// @param body the named part
        record Named<C extends MifosFlowContext>(String name, Definition<C> body) implements Definition<C> {
            /// Creates the node.
            ///
            /// @param name the name; not `null`
            /// @param body the named part; not `null`
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
    /// applications can customize or replace.
    interface Engine {

        /// Binds a flow that owns its transactions; same as `bind(definition, Placement.STANDALONE)`.
        ///
        /// @param <C> the context type
        /// @param definition the flow definition
        /// @return the executable flow
        /// @throws MifosFlowException if a rule is violated or a reference cannot be resolved
        default <C extends MifosFlowContext> MifosFlow<C> bind(Definition<C> definition) {
            return bind(definition, Placement.STANDALONE);
        }

        /// Validates `definition` and compiles it into an executable flow that runs at the given `placement`.
        ///
        /// All references are resolved and all structural rules are checked here, once, so problems surface at
        /// startup. Running the result has no interpretation overhead and can be repeated any number of times.
        ///
        /// @param <C> the context type
        /// @param definition the flow definition
        /// @param placement where the flow will run, relative to transactions owned by its caller
        /// @return the executable flow
        /// @throws MifosFlowException if a rule is violated or a reference cannot be resolved
        <C extends MifosFlowContext> MifosFlow<C> bind(Definition<C> definition, Placement placement);
    }

    /// The executor that runs [Definition.ForkJoin] forks, referenced by name or given directly.
    sealed interface ExecutorRef {
        /// The executor named `default`.
        ExecutorRef DEFAULT = new ByName("default");

        /// References an executor by name.
        ///
        /// @param name the executor name
        /// @return the reference
        static ExecutorRef of(String name) {
            return new ByName(name);
        }

        /// References an executor given directly.
        ///
        /// @param executor the executor
        /// @return the reference
        static ExecutorRef of(Executor executor) {
            return new Direct(executor);
        }

        /// An executor referenced by name, resolved by the [Engine].
        ///
        /// @param name the executor name
        record ByName(String name) implements ExecutorRef {
            /// Creates a reference.
            ///
            /// @param name the executor name; not `null`
            public ByName {
                requireNonNull(name, "name");
            }

            @Override
            @NonNull public String toString() {
                return name;
            }
        }

        /// An executor given directly.
        ///
        /// @param executor the executor
        record Direct(Executor executor) implements ExecutorRef {
            /// Creates a reference.
            ///
            /// @param executor the executor; not `null`
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

        /// Wraps the execution of `node`; the default just calls `next`.
        ///
        /// @param <C> the context type
        /// @param node the step, check or named part being run
        /// @param context the context the node is run with
        /// @param next runs the node (and any inner interceptors)
        /// @return the resulting context, normally the one returned by `next`
        default <C extends MifosFlowContext> C intercept(Node node, C context, MifosFlow<C> next) {
            return next.run(context);
        }

        /// Called after `failedAttempt` of a retry failed with a retryable `cause`, before waiting `delay`.
        ///
        /// @param node the retry node
        /// @param failedAttempt the number of the attempt that failed, starting at 1
        /// @param cause the failure
        /// @param delay how long the engine waits before the next attempt
        default void onRetry(Node node, int failedAttempt, RuntimeException cause, Duration delay) {}
    }

    /// Where a bound flow will run, relative to transactions owned by its caller.
    enum Placement {
        /// The flow owns its transactions: nothing is open when it starts. The default.
        STANDALONE,

        /// The flow runs inside a transaction owned by its caller, e.g. a BPMN engine running a `JavaDelegate` as part
        /// of a job. The whole flow is treated as being inside a boundary, so retries and fork/join are
        /// rejected
        /// when binding: the caller retries the whole unit (BPMN job retries) and the flow must stay on the
        /// caller's thread.
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

        /// Creates a policy.
        ///
        /// @param maxAttempts total number of attempts, including the first one; at least 1
        /// @param retryable which failures are retried; not `null`
        /// @param initialBackoff delay after the first failed attempt; not negative
        /// @param multiplier growth factor of the delay per failed attempt; at least 1
        /// @param maxBackoff upper bound for the delay; at least `initialBackoff`
        /// @param jitter fraction in `[0, 1]` by which each delay is randomly shortened
        /// @throws IllegalArgumentException if a value is out of range
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
        ///
        /// @param maxAttempts total number of attempts, including the first one; at least 1
        /// @param retryable which failures are retried
        /// @return a policy without backoff or jitter
        public static RetryPolicy on(int maxAttempts, Predicate<? super RuntimeException> retryable) {
            return new RetryPolicy(maxAttempts, retryable, Duration.ZERO, 1.0, Duration.ZERO, 0.0);
        }

        /// Exponential backoff: `initial`, `initial * multiplier`, ... capped at `max`.
        ///
        /// @param initial delay after the first failed attempt
        /// @param multiplier growth factor of the delay per failed attempt; at least 1
        /// @param max upper bound for the delay
        /// @return a copy of this policy with the given backoff
        public RetryPolicy withBackoff(Duration initial, double multiplier, Duration max) {
            return new RetryPolicy(maxAttempts, retryable, initial, multiplier, max, jitter);
        }

        /// Randomly shortens each delay by up to the given fraction.
        ///
        /// @param jitter fraction in `[0, 1]`
        /// @return a copy of this policy with the given jitter
        public RetryPolicy withJitter(double jitter) {
            return new RetryPolicy(maxAttempts, retryable, initialBackoff, multiplier, maxBackoff, jitter);
        }

        /// The delay before the attempt following `failedAttempt` (1-based).
        ///
        /// @param failedAttempt the number of the attempt that failed, starting at 1
        /// @param random the source of the jitter
        /// @return the delay; [Duration#ZERO] if this policy has no backoff
        public Duration delayAfter(int failedAttempt, RandomGenerator random) {
            if (initialBackoff.isZero()) {
                return Duration.ZERO;
            }
            double base = initialBackoff.toNanos() * Math.pow(multiplier, failedAttempt - 1.0);
            double capped = Math.min(base, (double) maxBackoff.toNanos());
            double factor = 1.0 - jitter * random.nextDouble();
            return Duration.ofNanos((long) (capped * factor));
        }
    }

    /// A [RetryPolicy], referenced by name (resolved by the [Engine], e.g. from configuration properties) or given
    /// directly.
    sealed interface RetryRef {
        /// Optimistic-locking and similar concurrency conflicts.
        RetryRef CONFLICT = new ByName("conflict");

        /// References a retry policy by name.
        ///
        /// @param name the policy name
        /// @return the reference
        static RetryRef of(String name) {
            return new ByName(name);
        }

        /// References a retry policy given directly.
        ///
        /// @param policy the policy
        /// @return the reference
        static RetryRef of(RetryPolicy policy) {
            return new Direct(policy);
        }

        /// A retry policy referenced by name, resolved by the [Engine].
        ///
        /// @param name the policy name
        record ByName(String name) implements RetryRef {
            /// Creates a reference.
            ///
            /// @param name the policy name; not `null`
            public ByName {
                requireNonNull(name, "name");
            }

            @Override
            @NonNull public String toString() {
                return name;
            }
        }

        /// A retry policy given directly.
        ///
        /// @param policy the policy
        record Direct(RetryPolicy policy) implements RetryRef {
            /// Creates a reference.
            ///
            /// @param policy the policy; not `null`
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
        /// Sleeps with [Thread#sleep(Duration)].
        Sleeper THREAD = Thread::sleep;

        /// Pauses for the given duration.
        ///
        /// @param duration how long to pause
        /// @throws InterruptedException if the thread is interrupted while pausing
        void sleep(Duration duration) throws InterruptedException;
    }

    /// Describes the node an interceptor is wrapping.
    ///
    /// @param name the step's usecase name, the name given with [Definition#named(String)], or for a retry the
    ///     retry policy reference
    /// @param kind `"step"`, `"check"`, `"named"` or `"retry"`
    /// @param path the position in the flow
    record Node(String name, String kind, String path) {
        /// Creates a node description.
        ///
        /// @param name the node name; not `null`
        /// @param kind the node kind; not `null`
        /// @param path the position in the flow; not `null`
        public Node {
            requireNonNull(name, "name");
            requireNonNull(kind, "kind");
            requireNonNull(path, "path");
        }
    }

    /// The flow DSL. Use with `import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.*`.
    ///
    /// Every method only builds a [Definition]; nothing runs until an [Engine] binds it. Definitions can
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
    @SuppressWarnings("UnescapedEntity") // Error Prone reads the Markdown code block above as HTML
    final class Dsl {
        private Dsl() {}

        // --- basics ---

        /// Does nothing.
        ///
        /// @param <C> the context type
        /// @return a [Definition.Noop]
        public static <C extends MifosFlowContext> Definition<C> noop() {
            return new Definition.Noop<>();
        }

        /// An ad-hoc transformation of the context; see [Definition.Act].
        ///
        /// @param <C> the context type
        /// @param fn the transformation
        /// @return a [Definition.Act]
        public static <C extends MifosFlowContext> Definition<C> act(Function<? super C, ? extends C> fn) {
            return new Definition.Act<>(fn);
        }

        /// A side effect that does not change the context; see [Definition.Tap].
        ///
        /// @param <C> the context type
        /// @param action the side effect
        /// @return a [Definition.Tap]
        public static <C extends MifosFlowContext> Definition<C> tap(Consumer<? super C> action) {
            return new Definition.Tap<>(action);
        }

        /// Names a part of the flow; same as `body.named(name)`.
        ///
        /// @param <C> the context type
        /// @param name the name
        /// @param body the named part
        /// @return a [Definition.Named]
        public static <C extends MifosFlowContext> Definition<C> named(String name, Definition<C> body) {
            return new Definition.Named<>(name, body);
        }

        /// Runs the steps in order.
        ///
        /// @param <C> the context type
        /// @param steps the steps
        /// @return a [Definition.Sequence]
        @SafeVarargs
        @SuppressWarnings("varargs")
        public static <C extends MifosFlowContext> Definition<C> sequence(Definition<C>... steps) {
            return new Definition.Sequence<>(List.of(steps));
        }

        /// Runs the steps in order.
        ///
        /// @param <C> the context type
        /// @param steps the steps
        /// @return a [Definition.Sequence]
        public static <C extends MifosFlowContext> Definition<C> sequence(List<Definition<C>> steps) {
            return new Definition.Sequence<>(steps);
        }

        // --- usecases ---

        /// A step with a usecase instance.
        ///
        /// @param <C> the context type
        /// @param <REQ> the usecase request type
        /// @param <RES> the usecase response type
        /// @param request derives the request from the context
        /// @param usecase the usecase
        /// @param merge merges the response into a new context
        /// @return a [Definition.Step]
        public static <C extends MifosFlowContext, REQ extends MifosRequest, RES extends MifosResponse>
                Definition<C> step(
                        Function<? super C, ? extends REQ> request,
                        MifosUsecase<? super REQ, ? extends RES> usecase,
                        BiFunction<? super C, ? super RES, ? extends C> merge) {
            return new Definition.Step<>(request, new UsecaseRef.Direct<>(usecase), merge);
        }

        /// A step with a usecase resolved by type when the flow is bound (in Spring: the bean of that type).
        ///
        /// @param <C> the context type
        /// @param <REQ> the usecase request type
        /// @param <RES> the usecase response type
        /// @param <U> the usecase type
        /// @param request derives the request from the context
        /// @param usecase the usecase type
        /// @param merge merges the response into a new context
        /// @return a [Definition.Step]
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

        /// A check with a verifying usecase instance; see [Definition.Check].
        ///
        /// @param <C> the context type
        /// @param <REQ> the usecase request type
        /// @param <RES> the usecase response type, ignored
        /// @param request derives the request from the context
        /// @param usecase the verifying usecase
        /// @return a [Definition.Check]
        public static <
                        C extends MifosFlowContext,
                        REQ extends MifosUsecase.MifosRequest,
                        RES extends MifosUsecase.MifosResponse>
                Definition<C> check(
                        Function<? super C, ? extends REQ> request, MifosUsecase<? super REQ, ? extends RES> usecase) {
            return new Definition.Check<>(request, new UsecaseRef.Direct<REQ, RES>(usecase));
        }

        /// A check with a verifying usecase resolved by type when the flow is bound (in Spring: the bean of that
        /// type).
        ///
        /// @param <C> the context type
        /// @param <REQ> the usecase request type
        /// @param <RES> the usecase response type, ignored
        /// @param <U> the usecase type
        /// @param request derives the request from the context
        /// @param usecase the usecase type
        /// @return a [Definition.Check]
        public static <
                        C extends MifosFlowContext,
                        REQ extends MifosUsecase.MifosRequest,
                        RES extends MifosUsecase.MifosResponse,
                        U extends MifosUsecase<REQ, RES>>
                Definition<C> check(Function<? super C, ? extends REQ> request, Class<U> usecase) {
            return new Definition.Check<>(request, new UsecaseRef.ByType<REQ, RES>(usecase));
        }

        /// Runs a flow with a different context type; see [Definition.Subflow].
        ///
        /// @param <C> the context type
        /// @param <D> the context type of the subflow
        /// @param enter maps the context into the subflow's context
        /// @param body the subflow
        /// @param exit merges the subflow's result back into the context
        /// @return a [Definition.Subflow]
        public static <C extends MifosFlowContext, D extends MifosFlowContext> Definition<C> subflow(
                Function<? super C, ? extends D> enter,
                Definition<D> body,
                BiFunction<? super C, ? super D, ? extends C> exit) {
            return new Definition.Subflow<>(enter, body, exit);
        }

        // --- decision and iteration ---

        /// Runs `then` if `condition` holds, `otherwise` if not.
        ///
        /// @param <C> the context type
        /// @param condition the condition
        /// @param then the branch taken when the condition holds
        /// @param otherwise the branch taken when it does not
        /// @return a [Definition.When]
        public static <C extends MifosFlowContext> Definition<C> when(
                Predicate<? super C> condition, Definition<C> then, Definition<C> otherwise) {
            return new Definition.When<>(condition, then, otherwise);
        }

        /// Runs `then` if `condition` holds, nothing if not.
        ///
        /// @param <C> the context type
        /// @param condition the condition
        /// @param then the branch taken when the condition holds
        /// @return a [Definition.When] with a [Definition.Noop] `otherwise` branch
        public static <C extends MifosFlowContext> Definition<C> when(
                Predicate<? super C> condition, Definition<C> then) {
            return when(condition, then, noop());
        }

        /// Runs `body` while `condition` holds, failing after `maxIterations` iterations.
        ///
        /// @param <C> the context type
        /// @param condition checked before every iteration
        /// @param maxIterations the maximum number of iterations; at least 1
        /// @param body the loop body
        /// @return a [Definition.RepeatWhile]
        public static <C extends MifosFlowContext> Definition<C> repeatWhile(
                Predicate<? super C> condition, int maxIterations, Definition<C> body) {
            return new Definition.RepeatWhile<>(condition, maxIterations, body);
        }

        /// Folds every item into the context.
        ///
        /// @param <C> the context type
        /// @param <I> the item type
        /// @param items extracts the items from the context
        /// @param fold merges an item into the context
        /// @return a [Definition.ForEach] with a [Definition.Noop] body
        public static <C extends MifosFlowContext, I> Definition<C> forEach(
                Function<? super C, ? extends Iterable<? extends I>> items,
                BiFunction<? super C, ? super I, ? extends C> fold) {
            return new Definition.ForEach<>(items, fold, noop());
        }

        /// Runs `body` once per item, after `enter` has put the item into the context.
        ///
        /// @param <C> the context type
        /// @param <I> the item type
        /// @param items extracts the items from the context
        /// @param enter puts an item into the context
        /// @param body runs once per item
        /// @return a [Definition.ForEach]
        public static <C extends MifosFlowContext, I> Definition<C> forEach(
                Function<? super C, ? extends Iterable<? extends I>> items,
                BiFunction<? super C, ? super I, ? extends C> enter,
                Definition<C> body) {
            return new Definition.ForEach<>(items, enter, body);
        }

        // --- parallelism ---

        /// Fork/join on the [ExecutorRef#DEFAULT] executor.
        ///
        /// @param <C> the context type
        /// @param snapshot creates the context each fork starts with
        /// @param forks the parallel parts
        /// @param merge combines two results
        /// @return a [Definition.ForkJoin]
        public static <C extends MifosFlowContext> Definition<C> forkJoin(
                Function<? super C, ? extends C> snapshot, List<Definition<C>> forks, BinaryOperator<C> merge) {
            return forkJoin(snapshot, forks, merge, ExecutorRef.DEFAULT);
        }

        /// Fork/join on the given executor; see [Definition.ForkJoin].
        ///
        /// @param <C> the context type
        /// @param snapshot creates the context each fork starts with
        /// @param forks the parallel parts
        /// @param merge combines two results
        /// @param executor the executor the forks run on
        /// @return a [Definition.ForkJoin]
        public static <C extends MifosFlowContext> Definition<C> forkJoin(
                Function<? super C, ? extends C> snapshot,
                List<Definition<C>> forks,
                BinaryOperator<C> merge,
                ExecutorRef executor) {
            return new Definition.ForkJoin<>(snapshot, forks, merge, executor);
        }

        // --- boundaries, retries, failures ---

        /// Runs `body` inside a boundary; same as `body.within(boundary)`.
        ///
        /// @param <C> the context type
        /// @param boundary the boundary
        /// @param body the wrapped part
        /// @return a [Definition.Within]
        public static <C extends MifosFlowContext> Definition<C> within(BoundaryRef boundary, Definition<C> body) {
            return body.within(boundary);
        }

        /// Retries `body`; same as `body.retryOn(policy)`.
        ///
        /// @param <C> the context type
        /// @param policy the retry policy reference
        /// @param body the retried part
        /// @return a [Definition.Retry]
        public static <C extends MifosFlowContext> Definition<C> retryOn(RetryRef policy, Definition<C> body) {
            return body.retryOn(policy);
        }

        /// Retries `body`; same as `body.retryOn(policy)`.
        ///
        /// @param <C> the context type
        /// @param policy the retry policy
        /// @param body the retried part
        /// @return a [Definition.Retry]
        public static <C extends MifosFlowContext> Definition<C> retryOn(RetryPolicy policy, Definition<C> body) {
            return body.retryOn(policy);
        }

        /// Every attempt runs in a fresh boundary: `body.within(boundary).retryOn(policy)`.
        ///
        /// @param <C> the context type
        /// @param boundary the boundary of each attempt
        /// @param policy the retry policy reference
        /// @param body the wrapped part
        /// @return a [Definition.Retry] around a [Definition.Within]
        public static <C extends MifosFlowContext> Definition<C> transactional(
                BoundaryRef boundary, RetryRef policy, Definition<C> body) {
            return body.within(boundary).retryOn(policy);
        }

        /// Recovers from failures of `body`; same as `body.recover(when, handler)`.
        ///
        /// @param <C> the context type
        /// @param body the guarded part
        /// @param when which failures to recover from; all others are rethrown
        /// @param handler computes the resulting context from the context `body` was started with and the
        /// failure
        /// @return a [Definition.Recover]
        public static <C extends MifosFlowContext> Definition<C> recover(
                Definition<C> body,
                Predicate<? super RuntimeException> when,
                BiFunction<? super C, ? super RuntimeException, ? extends C> handler) {
            return body.recover(when, handler);
        }

        /// Pairs an action with the step that undoes it, for use in a [#saga].
        ///
        /// @param <C> the context type
        /// @param action the action
        /// @param undo undoes the action
        /// @return a [Definition.Compensable]
        public static <C extends MifosFlowContext> Definition.Compensable<C> compensable(
                Definition<C> action, Definition<C> undo) {
            return new Definition.Compensable<>(action, undo);
        }

        /// Runs the actions in order, undoing the completed ones in reverse order if one fails.
        ///
        /// @param <C> the context type
        /// @param steps the actions with their undo steps
        /// @return a [Definition.Saga]
        @SafeVarargs
        @SuppressWarnings("varargs")
        public static <C extends MifosFlowContext> Definition<C> saga(Definition.Compensable<C>... steps) {
            return new Definition.Saga<>(List.of(steps));
        }

        /// Runs the actions in order, undoing the completed ones in reverse order if one fails.
        ///
        /// @param <C> the context type
        /// @param steps the actions with their undo steps
        /// @return a [Definition.Saga]
        public static <C extends MifosFlowContext> Definition<C> saga(List<Definition.Compensable<C>> steps) {
            return new Definition.Saga<>(steps);
        }
    }
}
