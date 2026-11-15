///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import java.util.Optional;
import org.mifos.commons.boot.operation.flow.MifosFlow;

/// Resolves retry policies referenced by name, e.g. from configuration properties.
@FunctionalInterface
public interface RetryPolicyResolver {
    Optional<MifosFlow.RetryPolicy> resolve(String name);
}
