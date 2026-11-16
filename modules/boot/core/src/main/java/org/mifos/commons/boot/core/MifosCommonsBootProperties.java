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
/// This record holds configuration settings that can be defined in the application's configuration files (e.g.,
/// `application.yml` or `application.properties`) under the prefix `mifos.commons`. It is bound by Spring Boot's
/// `@ConfigurationProperties` support.
///
/// Example usage in `application.yml`:
///
/// ```yaml
/// mifos:
///   commons:
///     error-url: "https://custom.domain/errors"
/// ```
///
/// @param errorUrl base URL of the error documentation, used e.g. as the type URI of problem details
/// @param tui the terminal UI type to use (see [MifosCommonsBootTuiType])
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
@ConfigurationProperties(prefix = MIFOS_COMMONS_BOOT_PROPERTIES_PREFIX)
public record MifosCommonsBootProperties(
        @DefaultValue("https://doc.mifos.org/projects/common/errors")
        String errorUrl,

        String tui) {
    /// Supported terminal UI implementations.
    public enum MifosCommonsBootTuiType {
        /// Picocli based command line interface.
        PICO,
        /// Spring Shell based interactive shell.
        SHELL,
        /// TamboUI based terminal UI.
        TAMBOUI
    }

    /// Transaction propagation behaviour of a flow boundary (mirrors Spring's transaction propagation types).
    public enum MifosCommonsBootFlowPropagation {
        /// Join the current transaction or create a new one if none exists.
        REQUIRED,
        /// Always create a new transaction, suspending the current one if present.
        REQUIRES_NEW,
        /// Join the current transaction; fail if none exists.
        MANDATORY,
        /// Join the current transaction if present; otherwise run non-transactionally.
        SUPPORTS,
        /// Run non-transactionally, suspending the current transaction if present.
        NOT_SUPPORTED,
        /// Run non-transactionally; fail if a transaction exists.
        NEVER,
        /// Run in a nested transaction (savepoint) if a transaction exists; otherwise like [#REQUIRED].
        NESTED
    }

    /// Transaction settings of a flow boundary.
    ///
    /// @param propagation transaction propagation behaviour
    /// @param readOnly whether the transaction is read-only
    /// @param timeout transaction timeout; `null` for the transaction manager's default
    /// @param transactionManager bean name of the transaction manager; empty for the primary one
    public record MifosCommonsBootFlowBoundaryProperties(
            @DefaultValue("required") MifosCommonsBootFlowPropagation propagation,
            @DefaultValue("false") boolean readOnly,
            Duration timeout,
            String transactionManager) {}

    /// Settings of a flow retry policy.
    ///
    /// @param maxAttempts total number of attempts, including the first one
    /// @param initialBackoff delay after the first failed attempt
    /// @param multiplier growth factor of the delay per failed attempt
    /// @param maxBackoff upper bound for the delay
    /// @param jitter fraction in `[0, 1]` by which each delay is randomly shortened
    /// @param retryOn fully qualified exception class names; empty means
    ///     `org.springframework.dao.ConcurrencyFailureException` (optimistic and pessimistic locking conflicts)
    public record MifosCommonsBootFlowRetryProperties(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("50ms") Duration initialBackoff,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("1s") Duration maxBackoff,
            @DefaultValue("0.5") double jitter,
            @DefaultValue List<String> retryOn) {}

    /// Settings of a flow executor.
    ///
    /// @param virtualThreads whether forks run on virtual threads
    /// @param concurrencyLimit maximum number of concurrently running forks; -1 for unlimited
    public record MifosCommonsBootFlowExecutorProperties(
            @DefaultValue("true") boolean virtualThreads,
            @DefaultValue("-1") int concurrencyLimit) {}
}
