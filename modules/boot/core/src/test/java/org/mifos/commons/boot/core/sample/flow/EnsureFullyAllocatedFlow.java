///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.exception.RepaymentNotAllowedException;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class EnsureFullyAllocatedFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        if (ctx.getRemaining().isPositive())
            throw new RepaymentNotAllowedException("Payment exceeds outstanding dues by "
                    + ctx.getRemaining().getNumberStripped().toPlainString() + " "
                    + ctx.getRemaining().getCurrency());
        return ctx;
    }
}
