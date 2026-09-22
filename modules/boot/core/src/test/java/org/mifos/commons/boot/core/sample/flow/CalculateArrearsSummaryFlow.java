///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import java.time.temporal.ChronoUnit;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.Money;
import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.ArrearsSummary;
import org.mifos.commons.boot.core.sample.model.Installment;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class CalculateArrearsSummaryFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        var overdue = ctx.getLoan().schedule().stream()
                .filter(i -> i.dueDate().isBefore(ctx.getBusinessDate()))
                .toList(); // schedule assumed sorted by dueDate

        if (overdue.isEmpty()) {
            ctx.setArrears(new ArrearsSummary(
                    0,
                    Money.zero(CurrencyUnitBuilder.of(ctx.getLoan().currency(), "default")
                            .build())));
            return ctx;
        }

        var overdueAmount = overdue.stream()
                .map(Installment::totalDue)
                .reduce(
                        Money.zero(CurrencyUnitBuilder.of(ctx.getLoan().currency(), "default")
                                .build()),
                        Money::add);
        long days = ChronoUnit.DAYS.between(overdue.getFirst().dueDate(), ctx.getBusinessDate());

        ctx.setArrears(new ArrearsSummary(days, overdueAmount));

        return ctx;
    }
}
