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

/// Utility class holding constant values used throughout Mifos Commons Boot.
///
/// This class defines package names, message bundle paths and message key prefixes, error code ranges, HTTP header
/// names and configuration property names.
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
    /// Base package of Mifos Commons Boot.
    public static final String MIFOS_COMMONS_BOOT_BASE_PACKAGE = MIFOS_PACKAGE_BASE + ".commons.boot";
    /// Package of the core module.
    public static final String MIFOS_COMMONS_BOOT_CORE_PACKAGE = MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".core";
    /// Package of the debug module.
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PACKAGE = MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".debug";
    /// Package of the CLI modules.
    public static final String MIFOS_COMMONS_BOOT_CLI_PACKAGE = MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".cli";
    /// Base resource path of the Mifos Commons message bundles.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_BASE = MIFOS_MESSAGE_BASE + "/commons/messages";
    /// Prefix of all Mifos Commons message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_PREFIX = MIFOS_PACKAGE_BASE + ".commons";
    /// Prefix of error message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX = MIFOS_COMMONS_BOOT_MESSAGE_PREFIX + ".error";
    /// Prefix of validation error message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_VALIDATION_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".validation";
    /// Prefix of operation error message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_OPERATION_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".operation";
    /// Prefix of use case error message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_USECASE_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".usecase";
    /// Prefix of flow error message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_FLOW_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".flow";
    /// Prefix of CLI error message keys.
    public static final String MIFOS_COMMONS_BOOT_MESSAGE_ERROR_CLI_PREFIX =
            MIFOS_COMMONS_BOOT_MESSAGE_ERROR_PREFIX + ".cli";
    /// First error code of Mifos Commons Boot (common errors).
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_START = 100_000_000;
    /// Default increment between consecutive error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT = 10;
    /// Increment between the start codes of error code ranges.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_CATEGORY_INCREMENT = 10_000;
    /// First validation error code.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_START = 100_010_000;
    /// Increment between consecutive validation error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_VALIDATION_INCREMENT =
            MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    /// First operation error code.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_OPERATION_START = 100_020_000;
    /// Increment between consecutive operation error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_OPERATION_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    /// First use case error code.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_START = 100_030_000;
    /// Increment between consecutive use case error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_USECASE_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    /// First flow error code.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_START = 100_040_000;
    /// Increment between consecutive flow error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_FLOW_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    /// First CLI error code.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_START = 100_050_000;
    /// Increment between consecutive CLI error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_CLI_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    /// First error code available to applications built on Mifos Commons Boot.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_EXTERNAL_START = 300_000_000;
    /// Increment between consecutive external error codes.
    public static final int MIFOS_COMMONS_BOOT_ERROR_CODE_EXTERNAL_INCREMENT = MIFOS_COMMONS_BOOT_ERROR_CODE_INCREMENT;
    /// Name of the problem detail property holding the individual errors.
    public static final String MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE = "errors";
    /// HTTP header carrying the tenant identifier.
    public static final String MIFOS_COMMONS_BOOT_HEADER_TENANT_ID = "x-mifos-tenant-id";
    /// Prefix of the Mifos Commons Boot configuration properties.
    public static final String MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX = MIFOS_PROPERTIES_PREFIX + ".commons";
    /// Prefix of the debug configuration properties.
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTIES_PREFIX =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".debug";
    /// Property enabling the Caffeine cache debug listener.
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_CACHE_CAFFEINE_ENABLED =
            MIFOS_COMMONS_BOOT_DEBUG_PROPERTIES_PREFIX + ".cache-caffeine-enabled";
    /// Property enabling the Ehcache cache debug listener.
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_CACHE_EHCACHE_ENABLED =
            MIFOS_COMMONS_BOOT_DEBUG_PROPERTIES_PREFIX + ".cache-ehcache-enabled";
    /// Property selecting the terminal UI type.
    public static final String MIFOS_COMMONS_BOOT_DEBUG_PROPERTY_TUI_TYPE =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".tui-type";
}
