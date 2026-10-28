///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.boot.commons.service;

import org.mifos.commons.boot.core.pattern.MifosUsecase;

final class DummyUsecase implements MifosUsecase<DummyUsecase.Request, DummyUsecase.Response> {
    record Request(String name) implements MifosUsecase.MifosRequest {}
    ;

    record Response(String name) implements MifosUsecase.MifosResponse {}
    ;

    @Override
    public Response execute(Request request) {
        return new Response(request.name());
    }
}
