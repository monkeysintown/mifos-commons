///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.flow.exception;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX;

import java.io.Serial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.core.exception.MifosBaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.model.MifosErrorCode;

public final class MifosFlowException extends MifosBaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    public MifosFlowException(MifosError error) {
        super(error);
    }

    @Getter
    @RequiredArgsConstructor
    public enum MifosFlowErrorCode implements MifosErrorCode {
        MIFOS_COMMONS_ERROR_FLOW_ITERATION_LIMIT(
                MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX + ".iteration.limit"),
        MIFOS_COMMONS_ERROR_FLOW_DEFINITION(
                MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START + 2,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX + ".definition"),
        ;

        private final int value;
        private final String key;

        @Override
        public String getName() {
            return name();
        }
    }
}
