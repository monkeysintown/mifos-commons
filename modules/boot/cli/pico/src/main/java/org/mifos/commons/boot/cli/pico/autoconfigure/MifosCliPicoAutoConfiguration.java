///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.pico.autoconfigure;

import static org.mifos.commons.boot.cli.pico.MifosCliPicoConstants.MIFOS_COMMONS_BOOT_CLI_PICO_COMMAND_PACKAGE;
import static org.mifos.commons.boot.cli.pico.MifosCliPicoConstants.MIFOS_COMMONS_BOOT_CLI_PICO_RUNNER_PACKAGE;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_TUI_TYPE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Slf4j
@RequiredArgsConstructor
@ComponentScan(MIFOS_COMMONS_BOOT_CLI_PICO_COMMAND_PACKAGE)
@ComponentScan(MIFOS_COMMONS_BOOT_CLI_PICO_RUNNER_PACKAGE)
@Import({MifosCliePicoConfiguration.class})
@ConditionalOnProperty(value = MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_TUI_TYPE, havingValue = "PICO")
final class MifosCliPicoAutoConfiguration {}
