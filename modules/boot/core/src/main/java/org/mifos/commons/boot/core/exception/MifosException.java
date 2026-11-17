///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.exception;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_START;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX;

import java.io.Serial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/// General purpose Mifos exception carrying a structured [MifosError].
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
public class MifosException extends MifosBaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates an exception for the given error.
    ///
    /// @param error the error describing the failure
    public MifosException(MifosError error) {
        super(error);
    }

    /// Common Mifos error codes.
    @Getter
    @RequiredArgsConstructor
    public enum MifosCommonErrorCode implements MifosError.Code {
        /// An unknown or unexpected error.
        MIFOS_COMMONS_ERROR_UNKNOWN(
                MIFOS_COMMONS_BOOT_ERROR_CODE_START, MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".unknown"),
        /// A requested resource was not found.
        MIFOS_COMMONS_ERROR_NOT_FOUND(
                MIFOS_COMMONS_BOOT_ERROR_CODE_START + MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".not-found");

        /// The numeric error code.
        private final int value;
        /// The message key of the error.
        private final String key;

        @Override
        public String getName() {
            return name();
        }
    }
}
