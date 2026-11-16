///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.exception;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_INCREMENT;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX;

import java.io.Serial;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.core.exception.MifosBaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.model.MifosErrorCode;

/// Thrown for invalid flow definitions, unresolvable references, invalid configuration and exceeded loop limits.
public final class MifosFlowException extends MifosBaseException {
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates an exception.
    ///
    /// @param error the error
    public MifosFlowException(MifosError error) {
        super(error);
    }

    /// Error codes of the flow module.
    @Getter
    @RequiredArgsConstructor
    public enum MifosFlowErrorCode implements MifosErrorCode {
        /// An unspecified flow error.
        MIFOS_COMMONS_ERROR_FLOW_UNKNOWN(
                MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START, MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX + ".unknown"),
        /// A configured class name (e.g. in `mifos.flow.retry.*.retry-on`) cannot be loaded.
        MIFOS_COMMONS_ERROR_FLOW_CLASS_UNKNOWN(
                MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START + MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX + ".class.unknown"),
        /// A `repeatWhile` loop reached its maximum number of iterations.
        MIFOS_COMMONS_ERROR_FLOW_ITERATION_LIMIT(
                MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START + 2 * MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX + ".iteration.limit"),
        /// A flow definition violates a structural rule or references something that cannot be resolved.
        MIFOS_COMMONS_ERROR_FLOW_DEFINITION(
                MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START + 3 * MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_INCREMENT,
                MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX + ".definition"),
        ;

        /// The numeric error code.
        private final int value;

        /// The message key.
        private final String key;

        @Override
        public String getName() {
            return name();
        }
    }
}
