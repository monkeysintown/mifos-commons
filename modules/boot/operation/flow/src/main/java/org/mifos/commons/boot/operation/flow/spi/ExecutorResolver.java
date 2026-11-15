///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import java.util.Optional;
import java.util.concurrent.Executor;

/// Resolves fork/join executors referenced by name.
@FunctionalInterface
public interface ExecutorResolver {
    Optional<Executor> resolve(String name);
}
