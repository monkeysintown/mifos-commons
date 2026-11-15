///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core;

import static org.mifos.commons.boot.core.MifosConstants.MIFOS_MESSAGE_BASE;
import static org.mifos.commons.boot.core.MifosConstants.MIFOS_PACKAGE_BASE;
import static org.mifos.commons.boot.core.MifosConstants.MIFOS_PROPERTIES_PREFIX;

import lombok.experimental.UtilityClass;

/// Utility class holding constant values used throughout the Mifos messaging system.
///
/// This class defines base paths, prefixes, and specific message keys for resolving internationalized messages and
/// error codes within the Mifos application.
///
/// Since this is a utility class (annotated with Lombok's `@UtilityClass`), it is not intended to be
/// instantiated, and no public constructor is provided.
///
/// @since 1.0.0
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @see java.util.ResourceBundle
/// @see lombok.experimental.UtilityClass
@UtilityClass
public class MifosCommonsBootConstants {
    public static final String MIFOS_COMMONS_BOOT_BASE_PACKAGE = MIFOS_PACKAGE_BASE + ".commons.boot";
    public static final String MIFOS_COMMONS_BOOT_CORE_PACKAGE = MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".core";
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PACKAGE = MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".debug";
    public static final String MIFOS_COMMONS_BOOT_CLI_PACKAGE = MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".cli";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_BASE = MIFOS_MESSAGE_BASE + "/commons/messages";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_PREFIX = MIFOS_PACKAGE_BASE + ".commons";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX = MIFOS_COMMONS_BOOT_MESSAGE_PREFIX + ".error";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_VALIDATION_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".validation";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_OPERATION_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".operation";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".usecase";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".flow";
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_CLI_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".cli";
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_START = 100_000_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT = 10;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_CATEGORY_INCREMENT = 10_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_START = 100_010_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_INCREMENT =
            MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_OPERATION_START = 100_020_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_OPERATION_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START = 100_030_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START = 100_040_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_START = 100_050_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_EXTERNAL_START = 300_000_000;
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_EXTERNAL_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    public static final String MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE = "errors";
    public static final String MIFOS_COMMONS_BOOT_HEADER_TENANT_ID = "x-mifos-tenant-id";
    public static final String MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX = MIFOS_PROPERTIES_PREFIX + ".commons";
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTIES_PREFIX =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".debug";
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_CACHE_CAFFEINE_ENABLED =
            MIFOS_COMMONS_BOOT_DEBUG_PROPERTIES_PREFIX + ".cache-caffeine-enabled";
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_CACHE_EHCACHE_ENABLED =
            MIFOS_COMMONS_BOOT_DEBUG_PROPERTIES_PREFIX + ".cache-ehcache-enabled";
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_TUI_TYPE =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".tui-type";
}
