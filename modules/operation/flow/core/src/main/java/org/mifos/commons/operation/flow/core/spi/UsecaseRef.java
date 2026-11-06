///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.operation.flow.core.spi;

import java.util.Objects;
import org.mifos.commons.operation.usecase.core.MifosUsecase;
import org.mifos.commons.operation.usecase.core.MifosUsecase.MifosRequest;
import org.mifos.commons.operation.usecase.core.MifosUsecase.MifosResponse;

/// A usecase, referenced by type (resolved by the [MifosFlowEngine], e.g. as a Spring bean) or given as an instance.
///
/// Referencing by type lets flow definitions be static constants that do not depend on Spring.
public sealed interface UsecaseRef<REQ extends MifosRequest, RES extends MifosResponse> {

    /// A short, readable name, used for interceptors and error messages.
    String name();

    record ByType<REQ extends MifosRequest, RES extends MifosResponse>(
            Class<? extends MifosUsecase<? super REQ, ? extends RES>> type) implements UsecaseRef<REQ, RES> {
        public ByType {
            Objects.requireNonNull(type, "type");
        }

        @Override
        public String name() {
            return type.getSimpleName();
        }
    }

    record Direct<REQ extends MifosRequest, RES extends MifosResponse>(MifosUsecase<? super REQ, ? extends RES> usecase)
            implements UsecaseRef<REQ, RES> {
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
