///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import java.util.Objects;
import org.mifos.commons.boot.operation.usecase.MifosUsecase;
import org.mifos.commons.boot.operation.usecase.MifosUsecase.MifosRequest;
import org.mifos.commons.boot.operation.usecase.MifosUsecase.MifosResponse;

/// A usecase, referenced by type (resolved by the [MifosFlow.Engine], e.g. as a Spring bean) or given as an
/// instance.
///
/// Referencing by type lets flow definitions be static constants that do not depend on Spring.
///
/// @param <REQ> the usecase request type
/// @param <RES> the usecase response type
public sealed interface UsecaseRef<REQ extends MifosRequest, RES extends MifosResponse> {

    /// A short, readable name, used for interceptors and error messages.
    ///
    /// @return the name
    String name();

    /// A usecase referenced by type, resolved when the flow is bound.
    ///
    /// @param <REQ> the usecase request type
    /// @param <RES> the usecase response type
    /// @param type the usecase type
    record ByType<REQ extends MifosRequest, RES extends MifosResponse>(
            Class<? extends MifosUsecase<? super REQ, ? extends RES>> type) implements UsecaseRef<REQ, RES> {
        /// Creates a reference.
        ///
        /// @param type the usecase type; not `null`
        public ByType {
            Objects.requireNonNull(type, "type");
        }

        @Override
        public String name() {
            return type.getSimpleName();
        }
    }

    /// A usecase given as an instance.
    ///
    /// @param <REQ> the usecase request type
    /// @param <RES> the usecase response type
    /// @param usecase the usecase
    record Direct<REQ extends MifosRequest, RES extends MifosResponse>(MifosUsecase<? super REQ, ? extends RES> usecase)
            implements UsecaseRef<REQ, RES> {
        /// Creates a reference.
        ///
        /// @param usecase the usecase; not `null`
        public Direct {
            Objects.requireNonNull(usecase, "usecase");
        }

        @Override
        public String name() {
            String name = usecase.getClass().getSimpleName();
            int lambda = name.indexOf("$$Lambda");
            return lambda > 0 ? name.substring(0, lambda) + "::lambda" : name;
        }
    }
}
