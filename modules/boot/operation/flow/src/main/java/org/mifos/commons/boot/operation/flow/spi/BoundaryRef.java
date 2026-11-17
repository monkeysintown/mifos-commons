///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NonNull;

/// Where a flow's unit of work starts and ends, referenced by name (resolved by the
/// [org.mifos.commons.boot.operation.flow.MifosFlow.Engine]) or given
/// directly.
///
/// Flow definitions use names so that they stay free of Spring; the starter maps the well-known names to
/// `TransactionTemplate`s with the matching propagation.
public sealed interface BoundaryRef {
    /// No boundary. Unlike every other boundary, this one does not count as "inside a boundary" for validation.
    BoundaryRef NONE = new DirectBoundaryRef(Boundary.NONE);

    /// Joins the current unit of work or starts a new one (`required`).
    BoundaryRef REQUIRED = new ByNameBoundaryRef("required");

    /// Always starts a new unit of work, suspending the current one (`requires-new`).
    BoundaryRef REQUIRES_NEW = new ByNameBoundaryRef("requires-new");

    /// A read-only unit of work (`read-only`).
    BoundaryRef READ_ONLY = new ByNameBoundaryRef("read-only");

    /// References a boundary by name.
    ///
    /// @param name the boundary name
    /// @return the reference
    static BoundaryRef of(String name) {
        return new ByNameBoundaryRef(name);
    }

    /// References a boundary given directly.
    ///
    /// @param boundary the boundary
    /// @return the reference
    static BoundaryRef of(Boundary boundary) {
        return new DirectBoundaryRef(boundary);
    }

    /// Whether this is [#NONE]. Decided by the reference alone, so validation never depends on how the engine
    /// resolves names: a definition that is invalid in production is also invalid in a test.
    ///
    /// Abstract rather than a default method: an interface with default methods is initialized together
    /// with
    /// its implementations, which could deadlock with the constants above (Error Prone
    /// `ClassInitializationDeadlock`).
    ///
    /// @return `true` if this reference means "no boundary"
    boolean isNone();

    /// A boundary referenced by name, resolved by the [org.mifos.commons.boot.operation.flow.MifosFlow.Engine].
    ///
    /// @param name the boundary name
    record ByNameBoundaryRef(String name) implements BoundaryRef {
        /// Creates a reference.
        ///
        /// @param name the boundary name; not `null`
        public ByNameBoundaryRef {
            requireNonNull(name, "name");
        }

        @Override
        public boolean isNone() {
            return false;
        }

        @Override
        @NonNull public String toString() {
            return name;
        }
    }

    /// A boundary given directly.
    ///
    /// @param boundary the boundary
    record DirectBoundaryRef(Boundary boundary) implements BoundaryRef {
        /// Creates a reference.
        ///
        /// @param boundary the boundary; not `null`
        public DirectBoundaryRef {
            requireNonNull(boundary, "boundary");
        }

        @Override
        public boolean isNone() {
            return Boundary.NONE.equals(boundary);
        }

        @Override
        @NonNull public String toString() {
            return isNone() ? "none" : "direct";
        }
    }
}
