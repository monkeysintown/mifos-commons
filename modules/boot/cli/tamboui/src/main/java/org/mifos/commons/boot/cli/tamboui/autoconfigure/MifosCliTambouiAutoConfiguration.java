///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.tamboui.autoconfigure;

import static org.mifos.commons.boot.cli.tamboui.MifosCliTambouiConstants.MIFOS_COMMONS_BOOT_CLI_TAMBOUI_COMMAND_PACKAGE;
import static org.mifos.commons.boot.cli.tamboui.MifosCliTambouiConstants.MIFOS_COMMONS_BOOT_CLI_TAMBOUI_ERROR_PACKAGE;
import static org.mifos.commons.boot.cli.tamboui.MifosCliTambouiConstants.MIFOS_COMMONS_BOOT_CLI_TAMBOUI_RUNNER_PACKAGE;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_TUI_TYPE;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Slf4j
@RequiredArgsConstructor
@ComponentScan(MIFOS_COMMONS_BOOT_CLI_TAMBOUI_COMMAND_PACKAGE)
@ComponentScan(MIFOS_COMMONS_BOOT_CLI_TAMBOUI_RUNNER_PACKAGE)
@ComponentScan(MIFOS_COMMONS_BOOT_CLI_TAMBOUI_ERROR_PACKAGE)
@Import({MifosCliTambouiConfiguration.class})
@ConditionalOnProperty(value = MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_TUI_TYPE, havingValue = "TAMBOUI")
final class MifosCliTambouiAutoConfiguration {}
