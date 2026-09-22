///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.tamboui.error;

import static dev.tamboui.tui.error.ErrorAction.SUPPRESS;
import static org.mifos.commons.boot.core.exception.MifosException.MifosCommonErrorCode.MIFOS_COMMONS_ERROR_UNKNOWN;

import dev.tamboui.tui.error.ErrorAction;
import dev.tamboui.tui.error.ErrorContext;
import dev.tamboui.tui.error.RenderError;
import dev.tamboui.tui.error.RenderErrorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.core.exception.MifosBaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
final class MifosCliTambouiErrorHandler implements RenderErrorHandler {
    private final ApplicationEventPublisher publisher;

    @Override
    public ErrorAction handle(RenderError error, ErrorContext context) {
        if (error.cause() instanceof MifosBaseException mfe) {
            publisher.publishEvent(mfe.getError());
        } else {
            publisher.publishEvent(MifosError.of(MIFOS_COMMONS_ERROR_UNKNOWN, error.cause()));
        }

        return SUPPRESS;
    }
}
