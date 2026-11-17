///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NonNull;
import org.mifos.commons.boot.operation.flow.MifosFlow;

/// A [MifosFlow.RetryPolicy], referenced by name (resolved by the [MifosFlow.Engine], e.g. from configuration
/// properties) or given
/// directly.
public sealed interface RetryRef {
    /// Optimistic-locking and similar concurrency conflicts.
    RetryRef CONFLICT = new ByName("conflict");

    /// References a retry policy by name.
    ///
    /// @param name the policy name
    /// @return the reference
    static RetryRef of(String name) {
        return new ByName(name);
    }

    /// References a retry policy given directly.
    ///
    /// @param policy the policy
    /// @return the reference
    static RetryRef of(MifosFlow.RetryPolicy policy) {
        return new Direct(policy);
    }

    /// A retry policy referenced by name, resolved by the [MifosFlow.Engine].
    ///
    /// @param name the policy name
    record ByName(String name) implements RetryRef {
        /// Creates a reference.
        ///
        /// @param name the policy name; not `null`
        public ByName {
            requireNonNull(name, "name");
        }

        @Override
        @NonNull public String toString() {
            return name;
        }
    }

    /// A retry policy given directly.
    ///
    /// @param policy the policy
    record Direct(MifosFlow.RetryPolicy policy) implements RetryRef {
        /// Creates a reference.
        ///
        /// @param policy the policy; not `null`
        public Direct {
            requireNonNull(policy, "policy");
        }

        @Override
        @NonNull public String toString() {
            return "direct";
        }
    }
}
