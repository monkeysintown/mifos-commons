///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.grpc.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_BASE_PACKAGE;
import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX;

import lombok.experimental.UtilityClass;

/// Constants for the gRPC transport modules: base package names and the configuration properties prefix.
///
/// @since 1.0.0
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
@UtilityClass
public class MifosTransportGrpcConstants {
    /// Base package of the gRPC transport modules.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_BASE_PACKAGE =
            MIFOS_COMMONS_BOOT_BASE_PACKAGE + ".transport.grpc";
    /// Package of the gRPC transport core module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_CORE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_BASE_PACKAGE + ".core";
    /// Package of the imperative gRPC transport module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_IMPERATIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_BASE_PACKAGE + ".imperative";
    /// Package of the reactive gRPC transport module.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_REACTIVE_PACKAGE =
            MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_BASE_PACKAGE + ".reactive";
    /// Prefix of the gRPC transport configuration properties.
    public static final String MIFOS_COMMONS_BOOT_TRANSPORT_GRPC_PROPERTIES_PREFIX =
            MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX + ".transport.grpc";
}
