///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import static org.mifos.commons.boot.core.pattern.MifosFlow.Dsl.forkJoin;
import static org.mifos.commons.boot.core.pattern.MifosFlow.Dsl.repeatWhile;
import static org.mifos.commons.boot.core.pattern.MifosFlow.Dsl.retry;
import static org.mifos.commons.boot.core.pattern.MifosFlow.Dsl.sequence;
import static org.mifos.commons.boot.core.pattern.MifosFlow.Dsl.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;
import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.BorrowerNotifier;
import org.mifos.commons.boot.core.sample.model.JournalEntryPoster;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class ProcessLoanRepaymentFlow implements MifosFlow<LoanRepaymentContext> {

    private final MifosFlow<LoanRepaymentContext> flow;

    public ProcessLoanRepaymentFlow(JournalEntryPoster poster, BorrowerNotifier notifier, Executor executor) {
        this.flow = sequence(
                new ValidateRepaymentFlow(),
                forkJoin(
                        LoanRepaymentContext::copy,
                        List.of(new CalculateArrearsSummaryFlow(), new CalculateOutstandingBalanceFlow()),
                        LoanRepaymentContext::mergeWith,
                        executor),
                when(ctx -> ctx.getArrears().daysInArrears() > 0, new AssessArrearsPenaltyFlow(new BigDecimal("0.01"))),
                new PrepareAllocationOrderFlow(),
                repeatWhile(
                        ctx -> ctx.getRemaining().isPositive() && ctx.hasNextComponent(),
                        new AllocateNextComponentFlow()),
                new EnsureFullyAllocatedFlow(),
                when(LoanRepaymentContext::isFullyAllocated, new CloseLoanFlow()),
                retry(3, new PostJournalEntriesFlow(poster)),
                new NotifyBorrowerFlow(notifier));
    }

    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        return flow.run(ctx);
    }
}
