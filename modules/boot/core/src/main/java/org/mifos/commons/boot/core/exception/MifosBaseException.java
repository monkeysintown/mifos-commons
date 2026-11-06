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
/// This exception enriches the standard runtime exception with a structured error code.
///
/// Subclasses must provide a concrete implementation by supplying a `prefix` and using a specific
/// [MifosError] enum value. The error message is resolved from resource bundles based on the `prefix` and
/// the error key, supporting internationalization. Optional arguments can be passed for parameterized messages.
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
/// @see RuntimeException
/// @see MifosError
public abstract class MifosBaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    // private static final String BOLT_SYMBOL = "\uD83C\uDF72"; // bolt symbol

    @Getter
    protected final MifosError error;

    protected MifosBaseException(MifosError error) {
        super(error.getCode().getKey(), error.getCause());
        this.error = error;
    }

    public String getCode() {
        return getCode(error.getCode().getValue());
    }

    public static String getCode(int code) {
        return StringUtils.leftPad(String.valueOf(code), 9, '0');
    }
}
