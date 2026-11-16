///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rsocket.core;

import static org.mifos.commons.boot.transport.rsocket.core.MifosTransportRsocketConstants.MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_PROPERTIES_PREFIX;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration properties for RSocket transport in Mifos Common.
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @see org.springframework.boot.context.properties.ConfigurationProperties
/// @since 1.0.0
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = MIFOS_COMMONS_BOOT_TRANSPORT_RSOCKET_PROPERTIES_PREFIX)
public class MifosTransportRsocketProperties {
    /// Base URL of the error documentation.
    @Builder.Default
    private String errorUrl = "https://doc.mifos.org/projects/common/errors";

    /// Names of the request headers to propagate.
    private Set<String> headers;

    /// Default page number used when a request does not specify one.
    @Builder.Default
    private int defaultPageNumber = 0;

    /// Default page size used when a request does not specify one.
    @Builder.Default
    private int defaultPageSize = 100;
}
