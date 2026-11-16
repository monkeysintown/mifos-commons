///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core;

import lombok.experimental.UtilityClass;

/// Utility class holding constant values used throughout the Mifos Platform Stack.
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
public class MifosConstants {
    /// Base Java package of all Mifos projects.
    public static final String MIFOS_PACKAGE_BASE = "org.mifos";
    /// Base resource path of Mifos message bundles.
    public static final String MIFOS_MESSAGE_BASE = "org/mifos";
    /// Root prefix of all Mifos configuration properties.
    public static final String MIFOS_PROPERTIES_PREFIX = "mifos";
    /// Property holding the text rendered as the Mifos startup banner.
    public static final String MIFOS_PROPERTY_BANNER = MIFOS_PROPERTIES_PREFIX + ".banner";

    /// Lightning symbol (U+1F5F2) used in console output.
    public static final String MIFOS_SYMBOL_THUNDERBOLT = "\uD83D\uDDF2";
    /// Globe symbol (U+1F30D) used in console output.
    public static final String MIFOS_SYMBOL_GLOBE = "\uD83C\uDF0D";
}
