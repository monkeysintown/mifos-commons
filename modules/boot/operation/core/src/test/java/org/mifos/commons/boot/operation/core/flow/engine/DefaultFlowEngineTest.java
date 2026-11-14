///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.flow.engine;

import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.act;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.check;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.compensable;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.forEach;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.forkJoin;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.noop;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.repeatWhile;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.saga;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.sequence;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.step;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.subflow;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.tap;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.transactional;
import static org.mifos.commons.boot.operation.core.flow.MifosFlow.Dsl.when;
import static org.mifos.commons.boot.operation.core.flow.exception.MifosFlowException.MifosFlowErrorCode.MIFOS_COMMONS_ERROR_FLOW_DEFINITION;
import static org.mifos.commons.boot.operation.core.flow.exception.MifosFlowException.MifosFlowErrorCode.MIFOS_COMMONS_ERROR_FLOW_ITERATION_LIMIT;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mifos.commons.boot.operation.core.flow.MifosFlow;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.BoundaryRef;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.ExecutorRef;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Interceptor;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.MifosFlowContext;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Placement;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.RetryPolicy;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.RetryRef;
import org.mifos.commons.boot.operation.core.flow.exception.MifosFlowException;
import org.mifos.commons.boot.operation.core.flow.spi.Boundary;
import org.mifos.commons.boot.operation.core.flow.spi.RetryPolicyResolver;
import org.mifos.commons.boot.operation.core.flow.spi.UsecaseResolver;
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase;

@Slf4j
@SuppressWarnings({"java:S5778", "java:S2925"})
final class DefaultFlowEngineTest {

    // --- fixtures ---

    record Ctx(int value, String trail) implements MifosFlowContext {
        static Ctx of(int value) {
            return new Ctx(value, "");
        }

        Ctx add(int delta) {
            return new Ctx(value + delta, trail);
        }

        Ctx mark(String s) {
            return new Ctx(value, trail + s);
        }
    }

    record Other(String text) implements MifosFlowContext {}

    record Amount(int value) implements MifosUsecase.MifosRequest {}

    record Doubled(int value) implements MifosUsecase.MifosResponse {}

    static final class DoubleAmount implements MifosUsecase<Amount, Doubled> {
        @Override
        public Doubled execute(Amount request) {
            return new Doubled(request.value() * 2);
        }
    }

    static final class RequirePositive implements MifosUsecase<Amount, Doubled> {
        @Override
        public Doubled execute(Amount request) {
            if (request.value() <= 0) {
                throw new BusinessException();
            }

            return new Doubled(request.value());
        }
    }

    static final class ConflictException extends RuntimeException {}

    static final class BusinessException extends RuntimeException {}

    static Definition<Ctx> mark(String s) {
        return act(ctx -> ctx.mark(s));
    }

    static Definition<Ctx> fail(Supplier<RuntimeException> failure) {
        return act(ctx -> {
            throw failure.get();
        });
    }

    static final class CountingBoundary implements Boundary {
        final AtomicInteger entered = new AtomicInteger();

        @Override
        public <T> T run(Supplier<T> work) {
            entered.incrementAndGet();

            return work.get();
        }
    }

    static final class RecordingInterceptor implements Interceptor {
        final List<String> events = new CopyOnWriteArrayList<>();
        final String label;

        RecordingInterceptor(String label) {
            this.label = label;
        }

        @Override
        public <C extends MifosFlowContext> C intercept(MifosFlow.Node node, C context, MifosFlow<C> next) {
            events.add(label + ":start " + node.name());

            try {
                C out = next.run(context);
                events.add(label + ":done " + node.name());

                return out;
            } catch (RuntimeException e) {
                events.add(label + ":fail " + node.name());

                throw e;
            }
        }

        @Override
        public void onRetry(MifosFlow.Node node, int failedAttempt, RuntimeException cause, Duration delay) {
            events.add(label + ":retry " + failedAttempt);
        }
    }

    static DefaultFlowEngine.Builder engine() {
        return DefaultFlowEngine.builder().withoutBoundaries();
    }

    static MifosFlow<Ctx> bind(Definition<Ctx> definition) {
        return engine().build().bind(definition);
    }

    // --- composition ---

    @Nested
    class Composition {
        @Test
        void sequenceAndAndThenRunInOrder() {
            assertEquals(
                    "abc",
                    bind(sequence(mark("a"), mark("b"), mark("c")))
                            .run(Ctx.of(0))
                            .trail());
            assertEquals(
                    "abc",
                    bind(mark("a").andThen(mark("b")).andThen(mark("c")))
                            .run(Ctx.of(0))
                            .trail());
        }

        @Test
        void andThenOnSequenceFlattens() {
            var definition = sequence(mark("a"), mark("b")).andThen(mark("c"));

            assertEquals(
                    3,
                    assertInstanceOf(Definition.Sequence.class, definition)
                            .steps()
                            .size());
        }

        @Test
        void whenChoosesBranch() {
            var flow = bind(when(c -> c.value() > 0, mark("pos"), mark("neg")));

            assertEquals("pos", flow.run(Ctx.of(1)).trail());
            assertEquals("neg", flow.run(Ctx.of(-1)).trail());
            assertEquals(
                    "", bind(when((Ctx c) -> false, mark("x"))).run(Ctx.of(0)).trail());
        }

        @Test
        void tapSeesContextButCannotChangeIt() {
            var seen = new AtomicInteger();
            var in = Ctx.of(7);

            assertSame(in, bind(tap((Ctx c) -> seen.set(c.value()))).run(in));
            assertEquals(7, seen.get());
        }

        @Test
        void subflowMapsContextInAndOut() {
            Definition<Other> shout = act(o -> new Other(o.text().toUpperCase(ROOT)));
            var flow = bind(subflow(c -> new Other(c.trail()), shout, (c, o) -> new Ctx(c.value(), o.text())));

            assertEquals("ABC", flow.run(new Ctx(0, "abc")).trail());
        }

        @Test
        void definitionsAreReusableAndBindIndependently() {
            var definition = mark("a");
            var first = bind(definition);
            var second = bind(definition);

            assertNotEquals(first, second);
            assertEquals("a", first.run(Ctx.of(0)).trail());
            assertEquals("a", second.run(Ctx.of(0)).trail());
        }
    }

    // --- usecases ---

    @Nested
    class Usecases {
        @Test
        void stepWithInstanceFoldsResponseIntoContext() {
            MifosFlow<Ctx> flow =
                    bind(step(c -> new Amount(c.value()), new DoubleAmount(), (c, r) -> new Ctx(r.value(), c.trail())));

            assertEquals(42, flow.run(Ctx.of(21)).value());
        }

        @Test
        void stepByTypeIsResolvedFromRegistration() {
            Definition<Ctx> definition =
                    step(c -> new Amount(c.value()), DoubleAmount.class, (c, r) -> new Ctx(r.value(), c.trail()));
            var flow = engine().usecase(DoubleAmount.class, new DoubleAmount())
                    .build()
                    .bind(definition);

            assertEquals(42, flow.run(Ctx.of(21)).value());
        }

        @Test
        void stepByTypeIsResolvedFromResolverOnceAtBindTime() {
            var lookups = new AtomicInteger();
            var resolver = new UsecaseResolver() {
                @Override
                public <U> Optional<U> resolve(Class<U> type) {
                    lookups.incrementAndGet();
                    return type == DoubleAmount.class ? Optional.of(type.cast(new DoubleAmount())) : Optional.empty();
                }
            };
            var flow = engine().usecases(resolver)
                    .build()
                    .<Ctx>bind(step(c -> new Amount(c.value()), DoubleAmount.class, (c, r) -> c.add(r.value())));
            flow.run(Ctx.of(1));
            flow.run(Ctx.of(1));

            assertEquals(1, lookups.get());
        }

        @Test
        void missingUsecaseFailsAtBindWithPath() {
            var definition = sequence(mark("a"), step(c -> new Amount(c.value()), DoubleAmount.class, (c, r) -> c))
                    .named("transfer");
            var e = assertThrows(MifosFlowException.class, () -> bind(definition));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_DEFINITION.getKey(), e.getMessage());
        }

        @Test
        void checkLeavesContextUnchangedOrThrows() {
            var flow = engine().usecase(RequirePositive.class, new RequirePositive())
                    .build()
                    .<Ctx>bind(check(c -> new Amount(c.value()), RequirePositive.class));
            var in = Ctx.of(1);

            assertSame(in, flow.run(in));
            assertThrows(BusinessException.class, () -> flow.run(Ctx.of(0)));
        }
    }

    // --- iteration ---

    @Nested
    class Iteration {
        @Test
        void repeatWhileStopsWhenConditionFails() {
            assertEquals(
                    5,
                    bind(repeatWhile(c -> c.value() < 5, 10, act(c -> c.add(1))))
                            .run(Ctx.of(0))
                            .value());
        }

        @Test
        void repeatWhileFailsInsteadOfLoopingForever() {
            var flow = bind(
                    repeatWhile((Ctx c) -> true, 3, act((Ctx c) -> c.add(1))).named("loop"));
            var e = assertThrows(MifosFlowException.class, () -> flow.run(Ctx.of(0)));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_ITERATION_LIMIT.getKey(), e.getMessage());
        }

        @Test
        void forEachFoldsItemsIntoContext() {
            assertEquals(
                    6,
                    bind(forEach(c -> List.of(1, 2, 3), Ctx::add))
                            .run(Ctx.of(0))
                            .value());
        }

        @Test
        void forEachRunsBodyPerItem() {
            var flow = forEach(c -> List.of("x", "y"), Ctx::mark, mark("."));

            assertEquals("x.y.", bind(flow).run(Ctx.of(0)).trail());
        }
    }

    // --- fork/join ---

    @Nested
    class ForkJoin {
        ExecutorService executor;

        @BeforeEach
        void start() {
            executor = Executors.newFixedThreadPool(4);
        }

        @AfterEach
        void stop() {
            executor.shutdownNow();
        }

        MifosFlow<Ctx> bindWithPool(Definition<Ctx> definition) {
            return engine().executor(ExecutorRef.DEFAULT.toString(), executor)
                    .build()
                    .bind(definition);
        }

        @Test
        void mergesInDeclarationOrderNotCompletionOrder() {
            Definition<Ctx> slow = act(ctx -> {
                sleep(100);
                return ctx.mark("slow");
            });
            var flow = bindWithPool(forkJoin(
                    c -> new Ctx(c.value(), ""),
                    List.of(slow, mark("fast")),
                    (acc, r) -> acc.mark("[" + r.trail() + "]")));

            assertEquals("[slow][fast]", flow.run(Ctx.of(0)).trail());
        }

        @Test
        void rethrowsOriginalExceptionUnwrapped() {
            MifosFlow<Ctx> flow = bindWithPool(forkJoin(c -> c, List.of(fail(ConflictException::new)), (a, b) -> a));
            assertThrows(ConflictException.class, () -> flow.run(Ctx.of(0)));
        }

        @Test
        void cancelsRemainingForksOnFailure() {
            var interrupted = new CountDownLatch(1);
            Definition<Ctx> blocker = act(ctx -> {
                try {
                    Thread.sleep(Duration.ofMinutes(1));
                } catch (InterruptedException _) {
                    interrupted.countDown();
                }
                return ctx;
            });
            Definition<Ctx> failing = act(ctx -> {
                sleep(50);
                throw new BusinessException();
            });
            MifosFlow<Ctx> flow = bindWithPool(forkJoin(c -> c, List.of(blocker, failing), (a, b) -> a));

            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                assertThrows(BusinessException.class, () -> flow.run(Ctx.of(0)));
                assertTrue(interrupted.await(5, TimeUnit.SECONDS), "blocking fork was not interrupted");
            });
        }

        @Test
        void insideBoundaryIsRejectedAtBind() {
            var definition = forkJoin(c -> c, List.of(mark("a")), (Ctx a, Ctx b) -> b)
                    .within(BoundaryRef.REQUIRED)
                    .named("report");
            var e = assertThrows(MifosFlowException.class, () -> bindWithPool(definition));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_DEFINITION.getKey(), e.getMessage());
        }

        @Test
        void insideNoneBoundaryIsAllowed() {
            var definition =
                    forkJoin(c -> c, List.of(mark("a")), (Ctx a, Ctx b) -> b).within(BoundaryRef.NONE);

            assertEquals("a", bindWithPool(definition).run(Ctx.of(0)).trail());
        }

        @Test
        void forksMayHaveTheirOwnBoundary() {
            var boundary = new CountingBoundary();
            var fork = mark("a").within(BoundaryRef.of(boundary));
            bindWithPool(forkJoin(c -> c, List.of(fork, fork), (Ctx a, Ctx b) -> b))
                    .run(Ctx.of(0));

            assertEquals(2, boundary.entered.get());
        }

        @Test
        void missingExecutorFailsAtBind() {
            var e = assertThrows(
                    MifosFlowException.class, () -> bind(forkJoin(c -> c, List.of(mark("a")), (a, b) -> b)));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_DEFINITION.getKey(), e.getMessage());
        }

        @Test
        void interceptorsApplyInsideForks() {
            var recorder = new RecordingInterceptor("i");
            var flow = engine().executor("default", executor)
                    .interceptor(recorder)
                    .build()
                    .bind(forkJoin(c -> c, List.of(mark("a").named("fork-a")), (Ctx a, Ctx b) -> b));
            flow.run(Ctx.of(0));

            assertEquals(List.of("i:start fork-a", "i:done fork-a"), recorder.events);
        }
    }

    // --- retries and boundaries ---

    @Nested
    class Retries {
        @Test
        void retriesOnlyMatchingFailures() {
            var attempts = new AtomicInteger();
            Definition<Ctx> flaky = act(ctx -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new ConflictException();
                }
                return ctx.mark("ok");
            });
            var flow = bind(flaky.retryOn(RetryPolicy.on(3, ConflictException.class::isInstance)));

            assertEquals("ok", flow.run(Ctx.of(0)).trail());
            assertEquals(3, attempts.get());
        }

        @Test
        void doesNotRetryBusinessFailures() {
            var attempts = new AtomicInteger();
            Definition<Ctx> failing = act(ctx -> {
                attempts.incrementAndGet();
                throw new BusinessException();
            });
            var flow = bind(failing.retryOn(RetryPolicy.on(5, ConflictException.class::isInstance)));

            assertThrows(BusinessException.class, () -> flow.run(Ctx.of(0)));
            assertEquals(1, attempts.get());
        }

        @Test
        void rethrowsLastFailureWhenAttemptsAreExhausted() {
            var attempts = new AtomicInteger();
            Definition<Ctx> failing = act(ctx -> {
                attempts.incrementAndGet();
                throw new ConflictException();
            });

            assertThrows(
                    ConflictException.class,
                    () -> bind(failing.retryOn(RetryPolicy.on(2, e -> true))).run(Ctx.of(0)));
            assertEquals(2, attempts.get());
        }

        @Test
        void transactionalUsesFreshBoundaryPerAttempt() {
            var boundary = new CountingBoundary();
            var attempts = new AtomicInteger();
            Definition<Ctx> flaky = act(ctx -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new ConflictException();
                }
                return ctx;
            });
            var flow = DefaultFlowEngine.builder()
                    .boundary(BoundaryRef.REQUIRED.toString(), boundary)
                    .retryPolicy(RetryRef.CONFLICT.toString(), RetryPolicy.on(3, ConflictException.class::isInstance))
                    .build()
                    .bind(transactional(BoundaryRef.REQUIRED, RetryRef.CONFLICT, flaky));
            flow.run(Ctx.of(0));

            assertEquals(3, boundary.entered.get());
        }

        @Test
        void retryInsideBoundaryIsRejectedAtBind() {
            var definition = mark("a").retryOn(RetryPolicy.on(3, e -> true)).within(BoundaryRef.REQUIRED);
            var e = assertThrows(MifosFlowException.class, () -> bind(definition));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_DEFINITION.getKey(), e.getMessage());
        }

        @Test
        void validationDoesNotDependOnHowBoundariesResolve() {
            // withoutBoundaries() resolves "required" to NONE, yet the definition is still rejected
            var definition = mark("a").retryOn(RetryPolicy.on(3, e -> true)).within(BoundaryRef.REQUIRED);

            assertThrows(MifosFlowException.class, () -> engine().build().bind(definition));
        }

        @Test
        void namedRetryPolicyIsResolvedOrFailsAtBind() {
            var definition = mark("a").retryOn(RetryRef.CONFLICT);

            assertThrows(MifosFlowException.class, () -> bind(definition));

            RetryPolicyResolver resolver = name -> Optional.of(RetryPolicy.on(2, e -> true));

            assertEquals(
                    "a",
                    engine().retryPolicies(resolver)
                            .build()
                            .bind(definition)
                            .run(Ctx.of(0))
                            .trail());
        }

        @Test
        void unknownNamedBoundaryFailsAtBind() {
            var definition = mark("a").within(BoundaryRef.of("ledger"));
            var e = assertThrows(
                    MifosFlowException.class,
                    () -> DefaultFlowEngine.builder().build().bind(definition));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_DEFINITION.getKey(), e.getMessage());
        }

        @Test
        void backoffUsesSleeperAndRetriesAreReported() {
            var slept = new CopyOnWriteArrayList<Duration>();
            var recorder = new RecordingInterceptor("i");
            var attempts = new AtomicInteger();
            var flaky = act(ctx -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new ConflictException();
                }
                return ctx;
            });
            var policy = RetryPolicy.on(3, e -> true).withBackoff(Duration.ofMillis(100), 2.0, Duration.ofSeconds(1));

            engine().sleeper(slept::add)
                    .interceptor(recorder)
                    .build()
                    .bind(flaky.retryOn(policy))
                    .run(Ctx.of(0));

            assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200)), slept);
            assertEquals(List.of("i:retry 1", "i:retry 2"), recorder.events);
        }
    }

    @Nested
    class RetryPolicies {
        @Test
        void backoffGrowsExponentiallyAndIsCapped() {
            RetryPolicy policy =
                    RetryPolicy.on(10, e -> true).withBackoff(Duration.ofMillis(100), 2.0, Duration.ofMillis(500));
            @SuppressWarnings("java:S2245") // deterministic seed for reproducible test data, not security-relevant
            var random = new Random(1);

            assertEquals(Duration.ofMillis(100), policy.delayAfter(1, random));
            assertEquals(Duration.ofMillis(200), policy.delayAfter(2, random));
            assertEquals(Duration.ofMillis(400), policy.delayAfter(3, random));
            assertEquals(Duration.ofMillis(500), policy.delayAfter(4, random));
        }

        @Test
        void jitterOnlyShortensDelay() {
            var policy = RetryPolicy.on(10, e -> true)
                    .withBackoff(Duration.ofMillis(100), 1.0, Duration.ofMillis(100))
                    .withJitter(0.5);
            @SuppressWarnings("java:S2245") // deterministic seed for reproducible test data, not security-relevant
            var random = new Random(42);

            for (int i = 0; i < 1000; i++) {
                Duration d = policy.delayAfter(1, random);
                assertTrue(
                        d.compareTo(Duration.ofMillis(50)) >= 0 && d.compareTo(Duration.ofMillis(100)) <= 0,
                        d::toString);
            }
        }

        @Test
        void rejectsInvalidSettings() {
            assertThrows(IllegalArgumentException.class, () -> RetryPolicy.on(0, e -> true));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RetryPolicy.on(1, e -> true).withBackoff(Duration.ofSeconds(2), 2.0, Duration.ofSeconds(1)));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RetryPolicy.on(1, e -> true).withJitter(1.5));
        }
    }

    // --- failures ---

    @Nested
    class Failures {
        @Test
        void recoverTurnsMatchingFailureIntoContext() {
            var flow = fail(BusinessException::new)
                    .recover(BusinessException.class::isInstance, (c, e) -> c.mark("rejected"));

            assertEquals("rejected", bind(flow).run(Ctx.of(0)).trail());
        }

        @Test
        void recoverRethrowsOtherFailures() {
            var flow = fail(ConflictException::new).recover(BusinessException.class::isInstance, (c, e) -> c);

            assertThrows(ConflictException.class, () -> bind(flow).run(Ctx.of(0)));
        }

        @Test
        void sagaUndoesCompletedStepsInReverseWithTheirContexts() {
            var undone = new CopyOnWriteArrayList<String>();
            Definition<Ctx> flowDefinition = saga(
                    compensable(mark("a"), tap(c -> undone.add("undo-a@" + c.trail()))),
                    compensable(mark("b"), tap(c -> undone.add("undo-b@" + c.trail()))),
                    compensable(fail(BusinessException::new), tap(c -> undone.add("undo-c"))));

            assertThrows(BusinessException.class, () -> bind(flowDefinition).run(Ctx.of(0)));
            assertEquals(List.of("undo-b@ab", "undo-a@a"), undone);
        }

        @Test
        void sagaAttachesUndoFailuresAsSuppressed() {
            var flow = saga(
                    compensable(mark("a"), fail(ConflictException::new)),
                    compensable(fail(BusinessException::new), noop()));
            var thrown = assertThrows(BusinessException.class, () -> bind(flow).run(Ctx.of(0)));

            assertEquals(1, thrown.getSuppressed().length);
            assertInstanceOf(ConflictException.class, thrown.getSuppressed()[0]);
        }

        @Test
        void sagaReturnsFinalContextOnSuccess() {
            var flowDefinition = saga(compensable(mark("a"), noop()), compensable(mark("b"), noop()));

            assertEquals("ab", bind(flowDefinition).run(Ctx.of(0)).trail());
        }
    }

    // --- placement ---

    @Nested
    class Placements {
        @Test
        void participatingRejectsRetryAtBind() {
            var definition = mark("a").retryOn(RetryPolicy.on(3, e -> true)).named("transfer");
            var e = assertThrows(
                    MifosFlowException.class, () -> engine().build().bind(definition, Placement.PARTICIPATING));

            assertEquals(MIFOS_COMMONS_ERROR_FLOW_DEFINITION.getKey(), e.getMessage());
        }

        @Test
        void participatingRejectsForkJoinAtBind() {
            var definition = forkJoin(c -> c, List.of(mark("a")), (Ctx a, Ctx b) -> b);

            assertThrows(
                    MifosFlowException.class,
                    () -> engine().executor("default", Runnable::run)
                            .build()
                            .bind(definition, Placement.PARTICIPATING));
        }

        @Test
        void sameDefinitionIsValidStandalone() {
            var definition = mark("a").retryOn(RetryPolicy.on(3, e -> true));

            assertEquals(
                    "a",
                    engine().build()
                            .bind(definition, Placement.STANDALONE)
                            .run(Ctx.of(0))
                            .trail());
            assertEquals("a", engine().build().bind(definition).run(Ctx.of(0)).trail());
        }

        @Test
        void participatingAllowsBoundariesRecoverAndSaga() {
            var definition = sequence(
                    mark("a").within(BoundaryRef.REQUIRES_NEW),
                    fail(BusinessException::new).recover(BusinessException.class::isInstance, (c, e) -> c.mark("r")),
                    saga(compensable(mark("s"), noop())));

            assertEquals(
                    "ars",
                    engine().build()
                            .bind(definition, Placement.PARTICIPATING)
                            .run(Ctx.of(0))
                            .trail());
        }
    }

    // --- interceptors ---

    @Nested
    class Interceptors {
        @Test
        void wrapNamedPartsAndStepsWithFirstRegisteredOutermost() {
            var outer = new RecordingInterceptor("outer");
            var inner = new RecordingInterceptor("inner");
            var shared = new CopyOnWriteArrayList<String>();
            var sharedOuter = new Interceptor() {
                @Override
                public <C extends MifosFlowContext> C intercept(MifosFlow.Node node, C ctx, MifosFlow<C> next) {
                    shared.add("outer " + node.name());
                    return outer.intercept(node, ctx, next);
                }
            };
            var sharedInner = new Interceptor() {
                @Override
                public <C extends MifosFlowContext> C intercept(MifosFlow.Node node, C ctx, MifosFlow<C> next) {
                    shared.add("inner " + node.name());
                    return inner.intercept(node, ctx, next);
                }
            };
            var flowDefinitiion = sequence(
                            step(c -> new Amount(c.value()), new DoubleAmount(), (c, r) -> c),
                            fail(BusinessException::new).named("second"))
                    .named("transfer");
            var flow = engine().interceptor(sharedOuter)
                    .interceptor(sharedInner)
                    .build()
                    .bind(flowDefinitiion);

            assertThrows(BusinessException.class, () -> flow.run(Ctx.of(1)));
            assertEquals(
                    List.of(
                            "outer transfer",
                            "inner transfer",
                            "outer DoubleAmount",
                            "inner DoubleAmount",
                            "outer second",
                            "inner second"),
                    shared);
            assertEquals(
                    List.of(
                            "outer:start transfer",
                            "outer:start DoubleAmount",
                            "outer:done DoubleAmount",
                            "outer:start second",
                            "outer:fail second",
                            "outer:fail transfer"),
                    outer.events);
        }

        @Test
        void nodeCarriesKindAndPath() {
            var nodes = new CopyOnWriteArrayList<MifosFlow.Node>();
            var capture = new Interceptor() {
                @Override
                public <C extends MifosFlowContext> C intercept(MifosFlow.Node node, C ctx, MifosFlow<C> next) {
                    nodes.add(node);
                    return next.run(ctx);
                }
            };
            var definition = step(c -> new Amount(c.value()), new DoubleAmount(), (Ctx c, Doubled r) -> c)
                    .within(BoundaryRef.REQUIRED)
                    .retryOn(RetryPolicy.on(2, e -> true))
                    .named("transfer");

            engine().interceptor(capture).build().bind(definition).run(Ctx.of(1));

            assertEquals(
                    List.of(
                            new MifosFlow.Node("transfer", "named", "transfer"),
                            new MifosFlow.Node(
                                    "DoubleAmount",
                                    "step",
                                    "transfer > retry(direct) > within(required) > DoubleAmount")),
                    nodes);
        }

        @Test
        void canShortCircuit() {
            var deny = new Interceptor() {
                @Override
                public <C extends MifosFlowContext> C intercept(MifosFlow.Node node, C ctx, MifosFlow<C> next) {
                    if (node.name().equals("secret")) {
                        throw new SecurityException("denied");
                    }

                    return next.run(ctx);
                }
            };
            var flow = engine().interceptor(deny).build().bind(mark("a").named("secret"));

            assertThrows(SecurityException.class, () -> flow.run(Ctx.of(0)));
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
