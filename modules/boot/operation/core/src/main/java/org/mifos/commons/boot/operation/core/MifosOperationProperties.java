///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// Configuration of the flow runtime.
///
/// ```yaml
/// mifos:
///   flow:
///     boundaries:
///       ledger-report: { propagation: requires-new, read-only: true, timeout: 30s }
///     retry:
///       conflict: { max-attempts: 5, initial-backoff: 20ms }
///       gateway-timeout:
///         max-attempts: 3
///         initial-backoff: 200ms
///         retry-on: [org.springframework.web.client.ResourceAccessException]
///     executors:
///       default: { virtual-threads: true }
///       io: { virtual-threads: true, concurrency-limit: 32 }
/// ```
///
/// The boundaries `required`, `requires-new` and `read-only` and the retry policy `conflict` exist even when not
/// configured; configuring them overrides the built-in settings.
@ConfigurationProperties("mifos.flow")
public record MifosOperationProperties(
        @DefaultValue Map<String, BoundaryProperties> boundaries,
        @DefaultValue Map<String, RetryProperties> retry,
        @DefaultValue Map<String, ExecutorProperties> executors) {

    public enum Propagation {
        REQUIRED,
        REQUIRES_NEW,
        MANDATORY,
        SUPPORTS,
        NOT_SUPPORTED,
        NEVER,
        NESTED
    }

    /// @param transactionManager bean name of the transaction manager; empty for the primary one
    public record BoundaryProperties(
            @DefaultValue("required") Propagation propagation,
            @DefaultValue("false") boolean readOnly,
            Duration timeout,
            String transactionManager) {}

    /// @param retryOn fully qualified exception class names; empty means
    ///     `org.springframework.dao.ConcurrencyFailureException` (optimistic and pessimistic locking conflicts)
    public record RetryProperties(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("50ms") Duration initialBackoff,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("1s") Duration maxBackoff,
            @DefaultValue("0.5") double jitter,
            @DefaultValue List<String> retryOn) {}

    /// @param concurrencyLimit maximum number of concurrently running forks; -1 for unlimited
    public record ExecutorProperties(
            @DefaultValue("true") boolean virtualThreads,
            @DefaultValue("-1") int concurrencyLimit) {}
}
