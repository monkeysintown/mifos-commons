///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;
import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.DueComponent;
import org.mifos.commons.boot.core.sample.model.DueComponentType;
import org.mifos.commons.boot.core.sample.model.Installment;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class PrepareAllocationOrderFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        var order = new ArrayList<DueComponent>();

        if (ctx.getLatePenalty() != null && ctx.getLatePenalty().isPositive()) {
            order.add(new DueComponent(0, DueComponentType.PENALTY, ctx.getLatePenalty())); // 0 = flow-assessed
        }

        for (var i : ctx.getLoan().schedule().stream()
                .sorted(comparing(Installment::dueDate))
                .toList()) {
            if (i.penaltyDue().isPositive()) {
                order.add(new DueComponent(i.number(), DueComponentType.PENALTY, i.penaltyDue()));
            }
            if (i.feeDue().isPositive()) {
                order.add(new DueComponent(i.number(), DueComponentType.FEE, i.feeDue()));
            }
            if (i.interestDue().isPositive()) {
                order.add(new DueComponent(i.number(), DueComponentType.INTEREST, i.interestDue()));
            }
            if (i.principalDue().isPositive()) {
                order.add(new DueComponent(i.number(), DueComponentType.PRINCIPAL, i.principalDue()));
            }
        }

        ctx.setAllocationOrder(List.copyOf(order));
        ctx.setRemaining(ctx.getPayment());

        return ctx;
    }
}
