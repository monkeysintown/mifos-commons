///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.BorrowerNotifier;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;
import org.mifos.commons.boot.core.sample.model.LoanStatus;

public class NotifyBorrowerFlow implements MifosFlow<LoanRepaymentContext> {
    private final BorrowerNotifier notifier;

    NotifyBorrowerFlow(BorrowerNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        String outcome = ctx.getLoan().status() == LoanStatus.CLOSED_OBLIGATIONS_MET
                ? "Loan is fully repaid and closed."
                : "Outstanding installments remain.";
        notifier.send("Loan " + ctx.getLoan().accountNo() + ": payment of "
                + ctx.totalAllocated().getNumberStripped().toPlainString() + " "
                + ctx.getLoan().currency()
                + " accepted. " + outcome);
        return ctx;
    }
}
