///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.model;

import static org.mifos.commons.boot.core.model.MifosErrorCategory.BUSINESS_RULE;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

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
    private MifosErrorCategory category;
    /// The error code.
    private MifosErrorCode code;
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
    /// @param category the error category
    /// @param code the error code
    /// @return the error
    public static MifosError of(MifosErrorCategory category, MifosErrorCode code) {
        return MifosError.builder()
                .category(category)
                .code(code)
                .args(List.of())
                .build();
    }

    /// Creates an error.
    ///
    /// @param category the error category
    /// @param code the error code
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(MifosErrorCategory category, MifosErrorCode code, List<Object> args) {
        return MifosError.builder().category(category).code(code).args(args).build();
    }

    /// Creates an error.
    ///
    /// @param category the error category
    /// @param code the error code
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(MifosErrorCategory category, MifosErrorCode code, Throwable cause) {
        return MifosError.builder().category(category).code(code).cause(cause).build();
    }

    /// Creates an error.
    ///
    /// @param category the error category
    /// @param code the error code
    /// @param cause the underlying cause
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(MifosErrorCategory category, MifosErrorCode code, Throwable cause, List<Object> args) {
        return MifosError.builder()
                .category(category)
                .code(code)
                .cause(cause)
                .args(args)
                .build();
    }

    /// Creates an error.
    ///
    /// @param category the error category
    /// @param code the error code
    /// @param field the field that caused the error
    /// @return the error
    public static MifosError of(MifosErrorCategory category, MifosErrorCode code, String field) {
        return MifosError.builder().category(category).code(code).field(field).build();
    }

    /// Creates an error.
    ///
    /// @param category the error category
    /// @param code the error code
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @return the error
    public static MifosError of(MifosErrorCategory category, MifosErrorCode code, String field, String message) {
        return MifosError.builder()
                .category(category)
                .code(code)
                .field(field)
                .message(message)
                .build();
    }

    /// Creates an error.
    ///
    /// @param category the error category
    /// @param code the error code
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(
            MifosErrorCategory category, MifosErrorCode code, String field, String message, Throwable cause) {
        return MifosError.builder()
                .category(category)
                .code(code)
                .field(field)
                .message(message)
                .cause(cause)
                .build();
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @return the error
    public static MifosError of(MifosErrorCode code) {
        return of(BUSINESS_RULE, code);
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(MifosErrorCode code, List<Object> args) {
        return of(BUSINESS_RULE, code, args);
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(MifosErrorCode code, Throwable cause) {
        return of(BUSINESS_RULE, code, cause);
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param cause the underlying cause
    /// @param args arguments for the parameterized error message
    /// @return the error
    public static MifosError of(MifosErrorCode code, Throwable cause, List<Object> args) {
        return of(BUSINESS_RULE, code, cause, args);
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param field the field that caused the error
    /// @return the error
    public static MifosError of(MifosErrorCode code, String field) {
        return of(BUSINESS_RULE, code, field);
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @return the error
    public static MifosError of(MifosErrorCode code, String field, String message) {
        return of(BUSINESS_RULE, code, field, message);
    }

    /// Creates an error in the [MifosErrorCategory#BUSINESS_RULE] category.
    ///
    /// @param code the error code
    /// @param field the field that caused the error
    /// @param message a human-readable error message
    /// @param cause the underlying cause
    /// @return the error
    public static MifosError of(MifosErrorCode code, String field, String message, Throwable cause) {
        return of(BUSINESS_RULE, code, field, message, cause);
    }
}
