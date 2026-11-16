///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.core.exception;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_INCREMENT;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_START;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_MESSAGE_ERROR_CLI_PREFIX;

import java.io.Serial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.core.exception.MifosBaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.model.MifosErrorCode;

/// Exception thrown by Mifos CLI components.
public class MifosCliException extends MifosBaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates a new CLI exception.
    ///
    /// @param error the structured error describing the failure
    public MifosCliException(MifosError error) {
        super(error);
    }

    /// Error codes for CLI related failures.
    @Getter
    @RequiredArgsConstructor
    public enum MifosCliErrorCode implements MifosErrorCode {
        /// Unknown CLI error.
        MIFOS_COMMONS_ERROR_CLI_UNKNOWN(
                MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_START, MIFOS_COMMONS_BOOT_MESSAGE_ERROR_CLI_PREFIX + ".unknown"),
        /// Generic CLI error.
        MIFOS_COMMONS_ERROR_CLI_GENERIC(
                MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_START + 2 * MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_CLI_PREFIX + ".generic"),
        ;

        /// Numeric error code.
        private final int value;

        /// Message key used to resolve the localized error message.
        private final String key;

        /// Returns the name of the enum constant.
        ///
        /// @return the enum constant name
        @Override
        public String getName() {
            return name();
        }
    }
}
