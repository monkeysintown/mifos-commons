///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow;

import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.check;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.sequence;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.step;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.tap;

import java.io.Serial;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.mifos.commons.boot.operation.core.usecase.MifosUsecase;
import org.mifos.commons.boot.operation.flow.MifosFlow.Definition;
import org.mifos.commons.boot.operation.flow.MifosFlow.Interceptor;
import org.mifos.commons.boot.operation.flow.MifosFlow.MifosFlowContext;
import org.mifos.commons.boot.operation.flow.MifosFlow.Node;
import org.mifos.commons.boot.operation.flow.engine.DefaultFlowEngine;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/// What does writing business logic as a bound flow cost compared to one hand-written method?
///
/// The same transfer is implemented three ways:
///
/// - `inline`: plain Java in one method, no usecase objects; the floor.
/// - `usecases`: one method calling the same usecase objects with the same request and response records, but
///   keeping state in local variables instead of copying an immutable context after every step.
/// - `flow*`: the bound `Definition`, without interceptors, with a delegating interceptor, and with an
///   interceptor that times every step (a lower bound for a Micrometer `Observation`).
///
/// `work` simulates the business logic inside each usecase in JMH CPU tokens (0 isolates the pure overhead).
/// Real usecases usually wait for a database, which costs milliseconds and dwarfs every number here; this
/// benchmark measures the part the flow adds, not the part a usecase does.
///
/// The `failure*` benchmarks run the same transfer with insufficient funds: a business exception thrown four
/// steps in and recovered, with and without a captured stack trace. `bindPerCall` shows why flows must be bound
/// once and kept.
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class FlowBenchmark {

    // --- domain ---

    record AccountRequest(long accountId) implements MifosUsecase.MifosRequest {}

    /// The flow context: an account transfer.
    ///
    /// @param accountId the account
    /// @param amount the amount to transfer
    /// @param balance the loaded account balance
    /// @param fee the computed fee
    /// @param limit the loaded account limit
    /// @param risk the risk score
    /// @param flagged whether the transfer was flagged as risky
    /// @param entryId the id of the posted journal entry
    /// @param newBalance the balance after the transfer
    /// @param status the transfer status
    public record Transfer(
            long accountId,
            long amount,
            long balance,
            long fee,
            long limit,
            int risk,
            boolean flagged,
            String entryId,
            long newBalance,
            String status)
            implements MifosFlowContext {

        static Transfer of(long accountId, long amount) {
            return new Transfer(accountId, amount, 0, 0, 0, 0, false, null, 0, "NEW");
        }

        Transfer withBalance(long v) {
            return new Transfer(accountId, amount, v, fee, limit, risk, flagged, entryId, newBalance, status);
        }

        Transfer withLimit(long v) {
            return new Transfer(accountId, amount, balance, fee, v, risk, flagged, entryId, newBalance, status);
        }

        Transfer withFee(long v) {
            return new Transfer(accountId, amount, balance, v, limit, risk, flagged, entryId, newBalance, status);
        }

        Transfer withRisk(int v) {
            return new Transfer(accountId, amount, balance, fee, limit, v, flagged, entryId, newBalance, status);
        }

        Transfer flag() {
            return new Transfer(accountId, amount, balance, fee, limit, risk, true, entryId, newBalance, status);
        }

        Transfer withEntry(String v) {
            return new Transfer(accountId, amount, balance, fee, limit, risk, flagged, v, newBalance, status);
        }

        Transfer withNewBalance(long v) {
            return new Transfer(accountId, amount, balance, fee, limit, risk, flagged, entryId, v, status);
        }

        Transfer withStatus(String v) {
            return new Transfer(accountId, amount, balance, fee, limit, risk, flagged, entryId, newBalance, v);
        }
    }

    record AmountRequest(long amount) implements MifosUsecase.MifosRequest {}

    record FundsRequest(long balance, long amount, long fee) implements MifosUsecase.MifosRequest {}

    record RiskRequest(long amount, long limit) implements MifosUsecase.MifosRequest {}

    record PostingRequest(long accountId, long amount, long fee) implements MifosUsecase.MifosRequest {}

    record BalanceRequest(long balance, long amount, long fee) implements MifosUsecase.MifosRequest {}

    record Amount(long value) implements MifosUsecase.MifosResponse {}

    record Score(int value) implements MifosUsecase.MifosResponse {}

    record Entry(String id) implements MifosUsecase.MifosResponse {}

    interface BusinessOutcome {
        String errorCode();
    }

    /// A business outcome; `stackTrace` false is the cheap form recommended for expected failures.
    static final class InsufficientFunds extends RuntimeException implements BusinessOutcome {
        @Serial
        private static final long serialVersionUID = 1L;

        InsufficientFunds(boolean stackTrace) {
            super("insufficient funds", null, false, stackTrace);
        }

        @Override
        public String errorCode() {
            return "INSUFFICIENT_FUNDS";
        }
    }

    // --- state ---

    /// Simulated business logic per usecase, in JMH CPU tokens; 0 isolates the pure overhead.
    @Param({"0", "1000"})
    public int work;

    MifosUsecase<AmountRequest, Amount> validate;
    MifosUsecase<AccountRequest, Amount> loadBalance;
    MifosUsecase<AccountRequest, Amount> loadLimit;
    MifosUsecase<AmountRequest, Amount> computeFee;
    MifosUsecase<FundsRequest, Amount> requireFunds;
    MifosUsecase<FundsRequest, Amount> requireFundsCheap;
    MifosUsecase<RiskRequest, Score> scoreRisk;
    MifosUsecase<PostingRequest, Entry> post;
    MifosUsecase<BalanceRequest, Amount> updateBalance;

    Definition<Transfer> definition;
    Definition<Transfer> recovering;
    Definition<Transfer> recoveringCheap;
    DefaultFlowEngine engine;
    MifosFlow<Transfer> flow;
    MifosFlow<Transfer> flowNoopInterceptor;
    MifosFlow<Transfer> flowTimingInterceptor;
    MifosFlow<Transfer> flowFailure;
    MifosFlow<Transfer> flowFailureCheap;
    final LongAdder timed = new LongAdder();

    static final Transfer OK = Transfer.of(42, 500);
    static final Transfer TOO_MUCH = Transfer.of(42, 1_000_000);

    /// Creates the usecases for the current `work` and binds the flows, once per trial.
    @Setup
    public void setUp() {
        int w = work;
        validate = r -> {
            Blackhole.consumeCPU(w);
            if (r.amount() <= 0) {
                throw new IllegalArgumentException("amount");
            }
            return new Amount(r.amount());
        };
        loadBalance = r -> {
            Blackhole.consumeCPU(w);
            return new Amount(r.accountId() * 7 % 10_000 + 5_000);
        };
        loadLimit = r -> {
            Blackhole.consumeCPU(w);
            return new Amount(50_000);
        };
        computeFee = r -> {
            Blackhole.consumeCPU(w);
            return new Amount(r.amount() / 100 + 1);
        };
        requireFunds = r -> {
            Blackhole.consumeCPU(w);
            if (r.balance() < r.amount() + r.fee()) {
                throw new InsufficientFunds(true);
            }
            return new Amount(r.balance());
        };
        requireFundsCheap = r -> {
            Blackhole.consumeCPU(w);
            if (r.balance() < r.amount() + r.fee()) {
                throw new InsufficientFunds(false);
            }
            return new Amount(r.balance());
        };
        scoreRisk = r -> {
            Blackhole.consumeCPU(w);
            return new Score((int) (r.amount() * 100 / r.limit()));
        };
        post = r -> {
            Blackhole.consumeCPU(w);
            return new Entry("JE-" + r.accountId());
        };
        updateBalance = r -> {
            Blackhole.consumeCPU(w);
            return new Amount(r.balance() - r.amount() - r.fee());
        };

        definition = transfer(requireFunds);
        recovering = definition.recover(BusinessOutcome.class::isInstance, (t, e) -> t.withStatus("DECLINED"));
        recoveringCheap = transfer(requireFundsCheap)
                .recover(BusinessOutcome.class::isInstance, (t, e) -> t.withStatus("DECLINED"));

        engine = DefaultFlowEngine.builder().build();
        flow = engine.bind(definition);
        flowNoopInterceptor = DefaultFlowEngine.builder()
                .interceptor(new Interceptor() {})
                .build()
                .bind(definition);
        flowTimingInterceptor = DefaultFlowEngine.builder()
                .interceptor(new Interceptor() {
                    @Override
                    public <C extends MifosFlowContext> C intercept(Node node, C context, MifosFlow<C> next) {
                        long start = System.nanoTime();
                        try {
                            return next.run(context);
                        } finally {
                            timed.add(System.nanoTime() - start);
                        }
                    }
                })
                .build()
                .bind(definition);
        flowFailure = engine.bind(recovering);
        flowFailureCheap = engine.bind(recoveringCheap);
    }

    /// Nine steps: a validation check, three lookups or calculations, a funds check, a risk score, a posting,
    /// a balance update and an empty tap. (The conditional flag is currently commented out.)
    Definition<Transfer> transfer(MifosUsecase<FundsRequest, Amount> funds) {
        return sequence(
                        check(t -> new AmountRequest(t.amount()), validate),
                        step(
                                t -> new AccountRequest(t.accountId()),
                                loadBalance,
                                (Transfer t, Amount a) -> t.withBalance(a.value())),
                        step(
                                t -> new AccountRequest(t.accountId()),
                                loadLimit,
                                (Transfer t, Amount a) -> t.withLimit(a.value())),
                        step(
                                t -> new AmountRequest(t.amount()),
                                computeFee,
                                (Transfer t, Amount a) -> t.withFee(a.value())),
                        check(t -> new FundsRequest(t.balance(), t.amount(), t.fee()), funds),
                        step(
                                t -> new RiskRequest(t.amount(), t.limit()),
                                scoreRisk,
                                (Transfer t, Score s) -> t.withRisk(s.value())),
                        // when("risky?", (Transfer t) -> t.risk() > 80, act(Transfer::flag)),
                        step(
                                t -> new PostingRequest(t.accountId(), t.amount(), t.fee()),
                                post,
                                (Transfer t, Entry e) -> t.withEntry(e.id())),
                        step(
                                t -> new BalanceRequest(t.balance(), t.amount(), t.fee()),
                                updateBalance,
                                (Transfer t, Amount a) -> t.withNewBalance(a.value())),
                        tap((Transfer t) -> {}))
                .named("transfer");
    }

    // --- baselines ---

    /// The floor: the same logic as plain Java, no usecase objects, no request or response records.
    ///
    /// @return the completed transfer
    @Benchmark
    public Transfer inline() {
        int w = work;
        Transfer t = OK;
        Blackhole.consumeCPU(w);
        if (t.amount() <= 0) {
            throw new IllegalArgumentException("amount");
        }
        Blackhole.consumeCPU(w);
        long balance = t.accountId() * 7 % 10_000 + 5_000;
        Blackhole.consumeCPU(w);
        long limit = 50_000;
        Blackhole.consumeCPU(w);
        long fee = t.amount() / 100 + 1;
        Blackhole.consumeCPU(w);
        if (balance < t.amount() + fee) {
            throw new InsufficientFunds(true);
        }
        Blackhole.consumeCPU(w);
        int risk = (int) (t.amount() * 100 / limit);
        boolean flagged = risk > 80;
        Blackhole.consumeCPU(w);
        String entry = "JE-" + t.accountId();
        Blackhole.consumeCPU(w);
        long newBalance = balance - t.amount() - fee;
        return new Transfer(
                t.accountId(), t.amount(), balance, fee, limit, risk, flagged, entry, newBalance, t.status());
    }

    /// One method calling the same usecases, with the same requests and responses, but mutable locals.
    ///
    /// @return the completed transfer
    @Benchmark
    public Transfer usecases() {
        Transfer t = OK;
        validate.execute(new AmountRequest(t.amount()));
        long balance = loadBalance.execute(new AccountRequest(t.accountId())).value();
        long limit = loadLimit.execute(new AccountRequest(t.accountId())).value();
        long fee = computeFee.execute(new AmountRequest(t.amount())).value();
        requireFunds.execute(new FundsRequest(balance, t.amount(), fee));
        int risk = scoreRisk.execute(new RiskRequest(t.amount(), limit)).value();
        boolean flagged = risk > 80;
        String entry =
                post.execute(new PostingRequest(t.accountId(), t.amount(), fee)).id();
        long newBalance = updateBalance
                .execute(new BalanceRequest(balance, t.amount(), fee))
                .value();
        return new Transfer(
                t.accountId(), t.amount(), balance, fee, limit, risk, flagged, entry, newBalance, t.status());
    }

    // --- flows ---

    /// The bound flow without interceptors.
    ///
    /// @return the completed transfer
    @Benchmark
    public Transfer flow() {
        return flow.run(OK);
    }

    /// The bound flow with an interceptor that only delegates.
    ///
    /// @return the completed transfer
    @Benchmark
    public Transfer flowNoopInterceptor() {
        return flowNoopInterceptor.run(OK);
    }

    /// The bound flow with an interceptor that times every step.
    ///
    /// @return the completed transfer
    @Benchmark
    public Transfer flowTimingInterceptor() {
        return flowTimingInterceptor.run(OK);
    }

    // --- failure path: a business outcome four steps in ---

    /// The usecases baseline up to the failing funds check, catching the exception (with stack trace).
    ///
    /// @return the declined transfer
    @Benchmark
    public Transfer failureUsecasesStackTrace() {
        Transfer t = TOO_MUCH;
        try {
            validate.execute(new AmountRequest(t.amount()));
            long balance =
                    loadBalance.execute(new AccountRequest(t.accountId())).value();
            loadLimit.execute(new AccountRequest(t.accountId()));
            long fee = computeFee.execute(new AmountRequest(t.amount())).value();
            requireFunds.execute(new FundsRequest(balance, t.amount(), fee));
            return t;
        } catch (InsufficientFunds _) {
            return t.withStatus("DECLINED");
        }
    }

    /// The bound flow recovering from the failing funds check; the exception captures a stack trace.
    ///
    /// @return the declined transfer
    @Benchmark
    public Transfer failureFlowStackTrace() {
        return flowFailure.run(TOO_MUCH);
    }

    /// The bound flow recovering from the failing funds check; the exception has no stack trace.
    ///
    /// @return the declined transfer
    @Benchmark
    public Transfer failureFlowNoStackTrace() {
        return flowFailureCheap.run(TOO_MUCH);
    }

    // --- the anti-pattern ---

    /// Binding on every call instead of once: resolution, validation and compilation each time.
    ///
    /// @return the completed transfer
    @Benchmark
    public Transfer bindPerCall() {
        return engine.bind(definition).run(OK);
    }
}
