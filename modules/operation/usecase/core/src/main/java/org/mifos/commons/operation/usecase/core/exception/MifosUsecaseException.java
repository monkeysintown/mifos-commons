///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.operation.usecase.core.exception;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX;

import java.io.Serial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.core.exception.MifosBaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.model.MifosErrorCode;

public class MifosUsecaseException extends MifosBaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public MifosUsecaseException(MifosError error) {
        super(error);
    }

    @Getter
    @RequiredArgsConstructor
    public enum MifosUsecaseErrorCode implements MifosErrorCode {
        MIFOS_COMMONS_ERROR_USECASE_UNKNOWN(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".unknown"),
        MIFOS_COMMONS_ERROR_USECASE_DUPLICATE(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 2 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".duplicate"),
        MIFOS_COMMONS_ERROR_USECASE_NOT_FOUND(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 3 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".not-found"),
        MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_NULL(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 4 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".request-type-null"),
        MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_ILLEGAL(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 5 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".request-type-illegal"),
        MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_NOT_FOUND(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 6 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".request-type-not-found"),
        MIFOS_COMMONS_ERROR_USECASE_REQUEST_INSTANCE_EMPTY(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 7 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".request-instance-empty"),
        MIFOS_COMMONS_ERROR_USECASE_RESPONSE_SERIALIZATION(
                MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START + 8 * MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX + ".response-serialization"),
        ;

        private final int value;
        private final String key;

        @Override
        public String getName() {
            return name();
        }
    }
}
