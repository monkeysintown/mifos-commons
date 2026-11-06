///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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
@ConfigurationProperties(prefix = MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX)
public record MifosCommonsBootProperties(
        @DefaultValue("https://doc.mifos.org/projects/common/errors")
        String errorUrl,

        String tui) {
    public enum MifosCommonsBootTuiType {
        PICO,
        SHELL,
        TAMBOUI
    }

    public enum MifosCommonsBootFlowPropagation {
        REQUIRED,
        REQUIRES_NEW,
        MANDATORY,
        SUPPORTS,
        NOT_SUPPORTED,
        NEVER,
        NESTED
    }

    /// @param transactionManager bean name of the transaction manager; empty for the primary one
    public record MifosCommonsBootFlowBoundaryProperties(
            @DefaultValue("required") MifosCommonsBootFlowPropagation propagation,
            @DefaultValue("false") boolean readOnly,
            Duration timeout,
            String transactionManager) {}

    /// @param retryOn fully qualified exception class names; empty means
    ///     `org.springframework.dao.ConcurrencyFailureException` (optimistic and pessimistic locking conflicts)
    public record MifosCommonsBootFlowRetryProperties(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("50ms") Duration initialBackoff,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("1s") Duration maxBackoff,
            @DefaultValue("0.5") double jitter,
            @DefaultValue List<String> retryOn) {}

    /// @param concurrencyLimit maximum number of concurrently running forks; -1 for unlimited
    public record MifosCommonsBootFlowExecutorProperties(
            @DefaultValue("true") boolean virtualThreads,
            @DefaultValue("-1") int concurrencyLimit) {}
}
