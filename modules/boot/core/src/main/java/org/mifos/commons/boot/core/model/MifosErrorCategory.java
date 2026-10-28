///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.model;

import org.springframework.http.HttpStatus;

public enum MifosErrorCategory {
    VALIDATION(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    CONFLICT(HttpStatus.CONFLICT),
    BUSINESS_RULE(HttpStatus.UNPROCESSABLE_CONTENT),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    DEPENDENCY_FAILURE(HttpStatus.BAD_GATEWAY),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    MifosErrorCategory(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
