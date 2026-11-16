///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.exception;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_INCREMENT;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_START;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_MESSAGE_ERROR_VALIDATION_PREFIX;

import java.io.Serial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.core.model.MifosErrorCode;
import org.springframework.validation.Errors;

/// Exception thrown when validation errors occur within the Mifos system.
///
/// This runtime exception encapsulates a collection of validation errors represented by an [Errors] object. It
/// is intended to be used during request validation to signal that the input data does not satisfy business
/// constraints.
///
/// The `errors` field is marked as `transient` to avoid serialization of the error details, as the
/// [Errors] object may not be serializable. The exception still carries the validation information in-memory for
/// handling within the same JVM instance.
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @see Errors
/// @since 1.0.0
@Getter
@RequiredArgsConstructor
public class MifosValidationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /// The validation errors.
    private final transient Errors errors;

    /// Validation error codes.
    @Getter
    @RequiredArgsConstructor
    public enum MifosValidationErrorCode implements MifosErrorCode {
        /// An unknown validation error.
        MIFOS_COMMONS_ERROR_VALIDATION_UNKNOWN(
                MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_START, MIFOS_COMMONS_BOOT_MESSAGE_ERROR_VALIDATION_PREFIX),
        /// A general validation error.
        MIFOS_COMMONS_ERROR_VALIDATION(
                MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_START + MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_VALIDATION_PREFIX),
        /// A constraint violation.
        MIFOS_COMMONS_ERROR_VALIDATION_CONSTRAINT(
                MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_START + 2 * MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_VALIDATION_PREFIX),
        ;

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
