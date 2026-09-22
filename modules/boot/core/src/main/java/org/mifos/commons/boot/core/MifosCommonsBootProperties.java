///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// Configuration properties for Mifos common functionality.
///
/// This class holds configuration settings that can be defined in the application's configuration files (e.g.,
/// `application.yml` or `application.properties`) under the prefix `mifos.common`. It is designed to
/// be used with Spring Boot's `@ConfigurationProperties` binding.
///
/// The class is annotated with Lombok annotations to reduce boilerplate:
///
///   - `@Builder` provides a builder pattern for constructing instances.
///   - `@Data` generates getters, setters, `toString()`, `equals()`, and `hashCode()` methods.
///   - `@NoArgsConstructor` and `@AllArgsConstructor` generate the corresponding constructors.
///
/// Example usage in `application.yml`:
///
/// ``````
///
/// mifos:
///   common:
///     error-url: "https://custom.domain/errors"
/// ```
/// ```
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX)
public class MifosCommonsBootProperties {
    @Builder.Default
    private String errorUrl = "https://doc.mifos.org/projects/common/errors";

    private String tui;

    public enum MifosCommonsBootTuiType {
        PICO,
        SHELL,
        TAMBOUI
    }
}
