///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.javamoney.moneta.Money;
import org.mifos.commons.boot.core.pattern.MifosFlow;
import org.mifos.commons.boot.core.sample.model.AllocationLine;
import org.mifos.commons.boot.core.sample.model.DueComponentType;
import org.mifos.commons.boot.core.sample.model.JournalEntry;
import org.mifos.commons.boot.core.sample.model.JournalEntryPoster;
import org.mifos.commons.boot.core.sample.model.LoanRepaymentContext;

public class PostJournalEntriesFlow implements MifosFlow<LoanRepaymentContext> {
    private final JournalEntryPoster poster;

    public PostJournalEntriesFlow(JournalEntryPoster poster) {
        this.poster = poster;
    }

    @Override
    public LoanRepaymentContext run(LoanRepaymentContext ctx) {
        var principal = sumAllocated(ctx, DueComponentType.PRINCIPAL);
        var income = sumAllocated(ctx, DueComponentType.INTEREST, DueComponentType.FEE, DueComponentType.PENALTY);
        var entries = new ArrayList<JournalEntry>();

        if (principal.isPositive()) {
            entries.add(new JournalEntry("100100 Cash", "112000 Loans Receivable", principal));
        }

        if (income.isPositive()) {
            entries.add(new JournalEntry("100100 Cash", "410100 Interest, Fee & Penalty Income", income));
        }

        ctx.setJournalEntries(List.copyOf(entries));

        poster.post(ctx.getJournalEntries());

        return ctx;
    }

    private static Money sumAllocated(LoanRepaymentContext ctx, DueComponentType... types) {
        var set = Set.of(types);
        return ctx.getAllocation().stream()
                .filter(line -> set.contains(line.component().type()))
                .map(AllocationLine::amountPaid)
                .reduce(
                        Money.zero(CurrencyUnitBuilder.of(ctx.getLoan().currency(), "default")
                                .build()),
                        Money::add);
    }
}
