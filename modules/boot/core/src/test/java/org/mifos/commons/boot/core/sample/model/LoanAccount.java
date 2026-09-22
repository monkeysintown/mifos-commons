///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.sample.model;

import java.util.List;

public record LoanAccount(Long id, String accountNo, String currency, LoanStatus status, List<Installment> schedule) {
    public LoanAccount withStatus(LoanStatus newStatus) {
        return new LoanAccount(id, accountNo, currency, newStatus, schedule);
    }
}
