///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.model;

import org.springframework.http.HttpStatus;

/// Category of a [MifosError], each mapped to the HTTP status reported for errors of that category.
public enum MifosErrorCategory {
    /// Invalid input; maps to `400 Bad Request`.
    VALIDATION(HttpStatus.BAD_REQUEST),
    /// Missing or invalid authentication; maps to `401 Unauthorized`.
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    /// Authenticated but not permitted; maps to `403 Forbidden`.
    FORBIDDEN(HttpStatus.FORBIDDEN),
    /// Requested resource does not exist; maps to `404 Not Found`.
    NOT_FOUND(HttpStatus.NOT_FOUND),
    /// Conflict with the current state of a resource; maps to `409 Conflict`.
    CONFLICT(HttpStatus.CONFLICT),
    /// Violation of a business rule; maps to `422 Unprocessable Content`.
    BUSINESS_RULE(HttpStatus.UNPROCESSABLE_CONTENT),
    /// Too many requests; maps to `429 Too Many Requests`.
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    /// Failure of a downstream dependency; maps to `502 Bad Gateway`.
    DEPENDENCY_FAILURE(HttpStatus.BAD_GATEWAY),
    /// Service temporarily unavailable; maps to `503 Service Unavailable`.
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
    /// Unexpected internal error; maps to `500 Internal Server Error`.
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    MifosErrorCategory(HttpStatus status) {
        this.status = status;
    }

    /// Returns the HTTP status associated with this category.
    ///
    /// @return the HTTP status
    public HttpStatus status() {
        return status;
    }
}
