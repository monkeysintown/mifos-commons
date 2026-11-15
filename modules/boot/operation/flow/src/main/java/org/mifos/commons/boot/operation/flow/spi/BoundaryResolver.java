///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import java.util.Optional;

/// Resolves boundaries referenced by name, e.g. `required` to a `TransactionTemplate` with `PROPAGATION_REQUIRED`.
@FunctionalInterface
public interface BoundaryResolver {
    Optional<Boundary> resolve(String name);
}
