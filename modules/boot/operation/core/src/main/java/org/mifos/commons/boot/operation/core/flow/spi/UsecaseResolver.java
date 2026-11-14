///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.flow.spi;

import java.util.Optional;

/// Resolves usecases referenced by type; in Spring, the unique bean of that type.
public interface UsecaseResolver {
    <U> Optional<U> resolve(Class<U> type);
}
