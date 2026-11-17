///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.exception;

import static org.mifos.commons.boot.core.exception.MifosError.Category.BUSINESS_RULE;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.http.HttpStatus;

/// Represents an error response returned by the Mifos platform.
///
/// This class encapsulates a single error detail, including its category, an error code, the field that caused the
/// error, a human-readable error message, message arguments and an optional cause. It is typically carried by Mifos
/// exceptions and rendered into error response payloads of Mifos APIs.
///
/// The class leverages Lombok annotations to reduce boilerplate code:
///
///   - `@Builder` – provides a builder pattern for convenient instance creation
///   - `@Data` – generates getters, setters, `equals()`, `hashCode()`, and `toString()`
///   - `@NoArgsConstructor` / `@AllArgsConstructor` – generates constructors with zero and all arguments
///   - `@FieldNameConstants` – generates an inner `Fields` class with field name constants
///
/// This class is [Serializable] to support distributed environments like session replication or message
/// passing.
///
/// @since 1.0.0
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @see Serializable
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
public class MifosError implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /// The error category.
    private Category category;
    /// The error code.
    private Code code;
    /// The field that caused the error, if any.
    private String field;
    /// A human-readable error message.
    private String message;
    /// Arguments for the parameterized error message.
    private List<Object> args;
    /// The underlying cause, if any.
    private Throwable cause;

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @return the error
    public static MifosError of(Code code, Category category) {
        return MifosError.builder()
                .category(category)
                .code(code)
                .args(List.of())
                .build();
    }

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(Code code, Category category, List<Object> args) {
        return MifosError.builder().category(category).code(code).args(args).build();
    }

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(Code code, Category category, Throwable cause) {
        return MifosError.builder().code(code).category(category).cause(cause).build();
    }

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @param cause the underlying cause
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(Code code, Category category, Throwable cause, List<Object> args) {
        return MifosError.builder()
                .code(code)
                .category(category)
                .cause(cause)
                .args(args)
                .build();
    }

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @param field the field that caused the error
    /// @return the error
    public static MifosError of(Code code, Category category, String field) {
        return MifosError.builder().code(code).category(category).field(field).build();
    }

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @return the error
    public static MifosError of(Code code, Category category, String field, String message) {
        return MifosError.builder()
                .code(code)
                .category(category)
                .field(field)
                .message(message)
                .build();
    }

    /// Creates an error.
    ///
    /// @param code the error code
    /// @param category the error category
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(Code code, Category category, String field, String message, Throwable cause) {
        return MifosError.builder()
                .code(code)
                .category(category)
                .field(field)
                .message(message)
                .cause(cause)
                .build();
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @return the error
    public static MifosError of(Code code) {
        return of(code, BUSINESS_RULE);
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(Code code, List<Object> args) {
        return of(code, BUSINESS_RULE, args);
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(Code code, Throwable cause) {
        return of(code, BUSINESS_RULE, cause);
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param cause the underlying cause
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(Code code, Throwable cause, List<Object> args) {
        return of(code, BUSINESS_RULE, cause, args);
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param field the field that caused the error
    /// @return the error
    public static MifosError of(Code code, String field) {
        return of(code, BUSINESS_RULE, field);
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @return the error
    public static MifosError of(Code code, String field, String message) {
        return of(code, BUSINESS_RULE, field, message);
    }

    /// Creates an error in the [Category#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(Code code, String field, String message, Throwable cause) {
        return of(code, BUSINESS_RULE, field, message, cause);
    }

    /// Category of a [MifosError], each mapped to the HTTP status reported for errors of that category.
    public enum Category {
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

        Category(HttpStatus status) {
            this.status = status;
        }

        /// Returns the HTTP status associated with this category.
        ///
        /// @return the HTTP status
        public HttpStatus status() {
            return status;
        }
    }

    /// A Mifos error code: a numeric value identifying the error together with a message key used to resolve the
    /// (localized) error message.
    public interface Code {
        /// Returns the numeric value of the error code.
        ///
        /// @return the numeric error code
        int getValue();

        /// Returns the symbolic name of the error code (typically the enum constant name).
        ///
        /// @return the error code name
        String getName();

        /// Returns the message key used to resolve the error message from the message bundles.
        ///
        /// @return the message key
        String getKey();
    }
}
