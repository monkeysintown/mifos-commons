///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.core;

import static org.mifos.commons.boot.transport.rest.core.MifosTransportRestConstants.MIFOS_COMMONS_BOOT_TRANSPORT_REST_PROPERTIES_PREFIX;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration properties for REST transport in Mifos Common.
///
/// This class holds all tunable parameters for REST communication, such as error documentation URL, custom HTTP
/// headers, and default pagination settings. The properties are bound under the prefix
/// `mifos.commons.transport.rest` in the application's configuration (e.g., `application.yml` or
/// `application.properties`).
///
/// Lombok annotations are used to generate a builder, getters/setters, constructors, and other boilerplate code.
/// Default values are provided for some fields via [lombok.Builder.Default].
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @see org.springframework.boot.context.properties.ConfigurationProperties
/// @since 1.0.0
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = MIFOS_COMMONS_BOOT_TRANSPORT_REST_PROPERTIES_PREFIX)
public class MifosTransportRestProperties {
    /// Base URL of the error documentation.
    @Builder.Default
    private String errorUrl = "https://doc.mifos.org/projects/common/errors";

    /// Lower-case names of the request headers to propagate into the request context.
    private Set<String> headers;

    /// Default page number used when a request does not specify one.
    @Builder.Default
    private int defaultPageNumber = 0;

    /// Default page size used when a request does not specify one.
    @Builder.Default
    private int defaultPageSize = 100;
}
