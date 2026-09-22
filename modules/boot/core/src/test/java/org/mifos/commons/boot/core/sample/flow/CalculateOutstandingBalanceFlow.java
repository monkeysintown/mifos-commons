///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.Money;
import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.Installment;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class CalculateOutstandingBalanceFlow implements MifosFlow<LoanRepaymentContext> {
    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        ctx.setOutstanding(ctx.getLoan().schedule().stream()
                .map(Installment::totalDue)
                .reduce(
                        Money.zero(CurrencyUnitBuilder.of(ctx.getLoan().currency(), "default")
                                .build()),
                        Money::add));
        return ctx;
    }
}
