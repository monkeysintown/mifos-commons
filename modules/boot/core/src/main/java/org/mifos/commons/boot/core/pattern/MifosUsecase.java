///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.pattern;

import java.io.Serializable;

/// Represents a use case in the Mifos platform, encapsulating a specific business operation.
///
/// This is a functional interface, allowing it to be used as the assignment target for lambda expressions or method
/// references. It defines a single abstract method [#execute(MifosRequest)] that takes a request and returns a
/// response, following the Command pattern or similar use-case-oriented design.
///
/// @param <REQ> the type of the request object that the use case expects as input
/// @param <RES> the type of the response object that the use case returns after execution
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
@FunctionalInterface
public interface MifosUsecase<REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse> {
    RES execute(REQ request);

    /// Marker interface for request objects
    interface MifosRequest extends Serializable {}

    /// Marker interface for response objects
    interface MifosResponse extends Serializable {}
}
