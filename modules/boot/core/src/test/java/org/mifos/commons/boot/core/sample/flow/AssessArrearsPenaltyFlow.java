///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import java.math.BigDecimal;
import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class AssessArrearsPenaltyFlow implements MifosFlow<LoanRepaymentContext> {
    private final BigDecimal rate;

    AssessArrearsPenaltyFlow(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        ctx.setLatePenalty(ctx.getArrears().overdueAmount().multiply(rate));
        return ctx;
    }
}
