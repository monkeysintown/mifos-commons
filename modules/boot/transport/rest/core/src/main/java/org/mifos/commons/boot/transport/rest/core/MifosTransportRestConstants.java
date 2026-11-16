///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_BASE_PACKAGE;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX;

import lombok.experimental.UtilityClass;

/// Constants for the REST transport modules: base package names and the configuration properties prefix.
///
/// Since this is a utility class (annotated with Lombok's `@UtilityClass`), it is not intended to be
/// instantiated, and no public constructor is provided.
///
/// @since 1.0.0
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
@UtilityClass
public class MifosTransportRestConstants {
    /// Base package of the REST transport modules.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_REST_BASE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".transport.rest";
    /// Package of the REST transport core module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_REST_CORE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_REST_BASE_PACKAGE + ".core";
    /// Package of the imperative REST transport module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_REST_IMPERATIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_REST_BASE_PACKAGE + ".imperative";
    /// Package of the reactive REST transport module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_REST_REACTIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_REST_BASE_PACKAGE + ".reactive";
    /// Prefix of the REST transport configuration properties.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_REST_PROPERTIES_PREFIX =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".transport.rest";
}
