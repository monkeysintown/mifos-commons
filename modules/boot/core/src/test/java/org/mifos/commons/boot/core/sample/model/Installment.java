///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.model;

import java.time.LocalDate;
import org.javamoney.moneta.Money;

public record Installment(
        int number, LocalDate dueDate, Money penaltyDue, Money feeDue, Money interestDue, Money principalDue) {
    public Money totalDue() {
        return penaltyDue.add(feeDue).add(interestDue).add(principalDue);
    }
}
