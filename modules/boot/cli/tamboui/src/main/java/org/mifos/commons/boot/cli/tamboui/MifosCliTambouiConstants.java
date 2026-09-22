///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.tamboui;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_CLI_PACKAGE;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MifosCliTambouiConstants {
    public static final String MIFOS_COMMONS_BOOT_CLI_TAMBOUI_PACKAGE = MIFOS_COMMONS_BOOT_CLI_PACKAGE + ".tamboui";
    public static final String MIFOS_COMMONS_BOOT_CLI_TAMBOUI_COMMAND_PACKAGE =
            MIFOS_COMMONS_BOOT_CLI_TAMBOUI_PACKAGE + ".command";
    public static final String MIFOS_COMMONS_BOOT_CLI_TAMBOUI_RUNNER_PACKAGE =
            MIFOS_COMMONS_BOOT_CLI_TAMBOUI_PACKAGE + ".runner";
    public static final String MIFOS_COMMONS_BOOT_CLI_TAMBOUI_ERROR_PACKAGE =
            MIFOS_COMMONS_BOOT_CLI_TAMBOUI_PACKAGE + ".error";
}
