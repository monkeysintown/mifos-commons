///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.Money;
import org.mifos.commons.boot.core.pattern.MifosFlow;

@Data
public class LoanRepaymentContext implements MifosFlow.MifosFlowContext {
    // --- input ---
    private LoanAccount loan; // non-final: CloseLoanFlow replaces it
    private final Money payment;
    private final LocalDate businessDate;

    // --- step results ---
    private ArrearsSummary arrears; // written by CalculateArrearsSummaryFlow (fork)
    private Money outstanding; // written by CalculateOutstandingBalanceFlow (fork)
    private Money latePenalty; // written by AssessArrearsPenaltyFlow (decision)
    private List<DueComponent> allocationOrder = List.of();
    private int cursor;
    private Money remaining;
    private final List<AllocationLine> allocation = new ArrayList<>();
    private List<JournalEntry> journalEntries = List.of();

    public LoanRepaymentContext(LoanAccount loan, Money payment, LocalDate businessDate) {
        this.loan = loan;
        this.payment = payment;
        this.businessDate = businessDate;
        this.remaining =
                Money.zero(CurrencyUnitBuilder.of(loan.currency(), "default").build());
    }

    /** Snapshot for a fork: fresh result slots, shared immutable input. */
    public LoanRepaymentContext copy() {
        return new LoanRepaymentContext(loan, payment, businessDate);
    }

    /** Merge a fork result back in — forks write disjoint fields (last write wins). */
    public LoanRepaymentContext mergeWith(LoanRepaymentContext forkResult) {
        if (forkResult.arrears != null) this.arrears = forkResult.arrears;
        if (forkResult.outstanding != null) this.outstanding = forkResult.outstanding;
        return this;
    }

    public boolean hasNextComponent() {
        return cursor < allocationOrder.size();
    }

    public boolean isFullyAllocated() {
        return !remaining.isPositive() && cursor >= allocationOrder.size();
    }

    public Money totalAllocated() {
        return allocation.stream()
                .map(AllocationLine::amountPaid)
                .reduce(
                        Money.zero(CurrencyUnitBuilder.of(loan.currency(), "default")
                                .build()),
                        Money::add);
    }

    public void cursorIncrement() {
        this.cursor++;
    }
}
