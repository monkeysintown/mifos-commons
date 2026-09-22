///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.exception.RepaymentNotAllowedException;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;
import org.mifos.commons.boot.core.sample.model.LoanStatus;

public class ValidateRepaymentFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        if (ctx.getLoan().status() != LoanStatus.ACTIVE)
            throw new RepaymentNotAllowedException("Loan " + ctx.getLoan().accountNo() + " is not active");
        if (!ctx.getPayment()
                .getCurrency()
                .getCurrencyCode()
                .equals(ctx.getLoan().currency()))
            throw new RepaymentNotAllowedException("Payment currency does not match loan currency");
        if (!ctx.getPayment().isPositive()) throw new RepaymentNotAllowedException("Payment amount must be positive");
        return ctx;
    }
}
