///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.AllocationLine;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class AllocateNextComponentFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        var next = ctx.getAllocationOrder().get(ctx.getCursor());
        var allocated = ctx.getRemaining().isLessThan(next.due()) ? ctx.getRemaining() : next.due();

        ctx.getAllocation().add(new AllocationLine(next, allocated));
        ctx.setRemaining(ctx.getRemaining().subtract(allocated));
        ctx.cursorIncrement();

        return ctx;
    }
}
