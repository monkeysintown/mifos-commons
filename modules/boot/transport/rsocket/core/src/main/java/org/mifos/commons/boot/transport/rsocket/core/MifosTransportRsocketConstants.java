///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rsocket.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_BASE_PACKAGE;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX;

import lombok.experimental.UtilityClass;

/// Constants for the RSocket transport modules: base package names and the configuration properties prefix.
///
/// @since 1.0.0
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
@UtilityClass
public class MifosTransportRsocketConstants {
    /// Base package of the RSocket transport modules.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_BASE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".transport.rsocket";
    /// Package of the RSocket transport core module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_CORE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_BASE_PACKAGE + ".core";
    /// Package of the reactive RSocket transport module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_REACTIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_BASE_PACKAGE + ".reactive";
    /// Prefix of the RSocket transport configuration properties.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_PROPERTIES_PREFIX =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".transport.rsocket";
}
