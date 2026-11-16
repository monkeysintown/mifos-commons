///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import java.util.function.Supplier;

/// A unit-of-work boundary around (part of) a flow, typically a database transaction.
///
/// Kept as a plain functional interface so that the core does not depend on Spring; the Spring starter adapts
/// it to `TransactionOperations`, tests use [#NONE].
@FunctionalInterface
public interface Boundary {
    /// No boundary: runs the work directly.
    Boundary NONE = Supplier::get;

    /// Runs `work` inside this boundary.
    ///
    /// @param <T> the result type
    /// @param work the work to run
    /// @return the result of `work`
    <T> T run(Supplier<T> work);
}
