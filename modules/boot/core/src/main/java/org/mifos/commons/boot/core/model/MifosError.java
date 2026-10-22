///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.model;

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
/// This class encapsulates a single error detail, including the field that caused the error, a human-readable error
/// message, and an internal error code. It is typically used within error response payloads from Mifos APIs.
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

    private MifosErrorCode code;
    private String field;
    private String message;
    private List<Object> args;
    private Throwable cause;

    public static MifosError of(MifosErrorCode code) {
        return MifosError.builder().code(code).args(List.of()).build();
    }

    public static MifosError of(MifosErrorCode code, List<Object> args) {
        return MifosError.builder().code(code).args(args).build();
    }

    public static MifosError of(MifosErrorCode code, Throwable cause) {
        return MifosError.builder().code(code).cause(cause).build();
    }

    public static MifosError of(MifosErrorCode code, Throwable cause, List<Object> args) {
        return MifosError.builder().code(code).cause(cause).args(args).build();
    }

    public static MifosError of(MifosErrorCode code, String field) {
        return MifosError.builder().code(code).field(field).build();
    }

    public static MifosError of(MifosErrorCode code, String field, String message) {
        return MifosError.builder().code(code).field(field).message(message).build();
    }

    public static MifosError of(MifosErrorCode code, String field, String message, Throwable cause) {
        return MifosError.builder()
                .code(code)
                .field(field)
                .message(message)
                .cause(cause)
                .build();
    }
}
