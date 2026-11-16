///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.core;

import static org.mifos.commons.boot.persistence.relational.core.MifosPersistenceConstants.MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_PROPERTIES_PREFIX;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration properties of the relational persistence modules (prefix `mifos.commons.persistence.relational`).
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = MIFOS_COMMONS_BOOT_PERSISTENCE_RELATIONAL_PROPERTIES_PREFIX)
public class MifosPersistenceProperties {
    /// Whether relational persistence is enabled.
    private boolean enabled;
}
