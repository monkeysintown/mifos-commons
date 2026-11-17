///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Executor;
import org.jspecify.annotations.NonNull;

/// The executor that runs [MifosFlow.Definition.ForkJoin] forks, referenced by name or given directly.
public sealed interface ExecutorRef {
    /// The executor named `default`.
    ExecutorRef DEFAULT = new ByName("default");

    /// References an executor by name.
    ///
    /// @param name the executor name
    /// @return the reference
    static ExecutorRef of(String name) {
        return new ByName(name);
    }

    /// References an executor given directly.
    ///
    /// @param executor the executor
    /// @return the reference
    static ExecutorRef of(Executor executor) {
        return new Direct(executor);
    }

    /// An executor referenced by name, resolved by the [MifosFlow.Engine].
    ///
    /// @param name the executor name
    record ByName(String name) implements ExecutorRef {
        /// Creates a reference.
        ///
        /// @param name the executor name; not `null`
        public ByName {
            requireNonNull(name, "name");
        }

        @Override
        @NonNull public String toString() {
            return name;
        }
    }

    /// An executor given directly.
    ///
    /// @param executor the executor
    record Direct(Executor executor) implements ExecutorRef {
        /// Creates a reference.
        ///
        /// @param executor the executor; not `null`
        public Direct {
            requireNonNull(executor, "executor");
        }

        @Override
        @NonNull public String toString() {
            return "direct";
        }
    }
}
