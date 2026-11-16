///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_BASE_PACKAGE;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX;

import lombok.experimental.UtilityClass;

/// Utility class holding constant values of the Mifos relational persistence modules.
///
/// This class defines the package names and the configuration property prefix of the relational persistence modules.
///
/// Since this is a utility class (annotated with Lombok's `@UtilityClass`), it is not intended to be
/// instantiated, and no public constructor is provided.
///
/// @since 1.0.0
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
@UtilityClass
public class MifosPersistenceConstants {
    /// Base package of the relational persistence modules.
    public static final String MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_BASE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".persistence.relational";
    /// Package of the relational persistence core module.
    public static final String MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_CORE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".core";
    /// Package of the imperative relational persistence module.
    public static final String MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_IMPERATIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".imperative";
    /// Package of the reactive relational persistence module.
    public static final String MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_REACTIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".reactive";
    /// Prefix of the relational persistence configuration properties.
    public static final String MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_PROPERTIES_PREFIX =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".persistence.relational";
}
