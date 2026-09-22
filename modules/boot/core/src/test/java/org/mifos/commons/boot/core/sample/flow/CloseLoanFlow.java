///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;
import org.mifos.commons.boot.core.sample.model.LoanStatus;

public class CloseLoanFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        ctx.setLoan(ctx.getLoan().withStatus(LoanStatus.CLOSED_OBLIGATIONS_MET));
        return ctx;
    }
}
