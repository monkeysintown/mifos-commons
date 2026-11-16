///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.exception;

import java.io.Serial;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.mifos.commons.boot.core.model.MifosError;

/// Abstract base class for all custom exceptions thrown by the Mifos system.
///
/// This exception enriches the standard runtime exception with a structured [MifosError] (category, error code,
/// message arguments, cause).
///
/// The exception message is the message key of the error code; the localized message is resolved from resource
/// bundles using that key and the optional error arguments.
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
/// @see RuntimeException
/// @see MifosError
public abstract class MifosBaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    // private static final String BOLT_SYMBOL = "\uD83C\uDF72"; // bolt symbol

    /// The error describing the failure.
    @Getter
    protected final MifosError error;

    /// Creates an exception for the given error; the message is the error code's message key.
    ///
    /// @param error the error describing the failure
    protected MifosBaseException(MifosError error) {
        super(error.getCode().getKey(), error.getCause());
        this.error = error;
    }

    /// Returns the numeric error code formatted as a zero-padded, 9-digit string.
    ///
    /// @return the formatted error code
    public String getCode() {
        return getCode(error.getCode().getValue());
    }

    /// Formats a numeric error code as a zero-padded, 9-digit string.
    ///
    /// @param code the numeric error code
    /// @return the formatted error code
    public static String getCode(int code) {
        return StringUtils.leftPad(String.valueOf(code), 9, '0');
    }
}
