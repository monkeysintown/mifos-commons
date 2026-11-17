///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.starter.flow;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/// Configuration of the flow runtime.
///
/// ```yaml
/// mifos:
///   operation:
///     flow:
///       boundaries:
///         ledger-report: { propagation: requires-new, read-only: true, timeout: 30s }
///       retry:
///         conflict: { max-attempts: 5, initial-backoff: 20ms }
///         gateway-timeout:
///           max-attempts: 3
///           initial-backoff: 200ms
///           retry-on: [org.springframework.web.client.ResourceAccessException]
///       executors:
///         default: { virtual-threads: true }
///         io: { virtual-threads: true, concurrency-limit: 32 }
/// ```
///
/// The boundaries `required`, `requires-new` and `read-only` and the retry policy `conflict` exist even when not
/// configured; configuring them overrides the built-in settings.
///
/// @param boundaries named unit-of-work boundaries (`mifos.flow.boundaries.*`)
/// @param retry named retry policies (`mifos.operation.flow.retry.*`)
/// @param executors named fork/join executors (`mifos.operation.flow.executors.*`)
@ConfigurationProperties("mifos.operation.flow")
public record MifosFlowProperties(
        @DefaultValue Map<String, BoundaryProperties> boundaries,
        @DefaultValue Map<String, RetryProperties> retry,
        @DefaultValue Map<String, ExecutorProperties> executors,
        @DefaultValue("KUML") RendererType rendererType) {

    public enum RendererType {
        PLANTUML,
        KUML
    }

    /// Transaction propagation of a boundary; mirrors Spring's `TransactionDefinition.PROPAGATION_*` constants.
    public enum Propagation {
        /// Joins the current transaction or starts a new one.
        REQUIRED,
        /// Always starts a new transaction, suspending the current one.
        REQUIRES_NEW,
        /// Joins the current transaction; fails if there is none.
        MANDATORY,
        /// Joins the current transaction; runs without one if there is none.
        SUPPORTS,
        /// Runs without a transaction, suspending the current one.
        NOT_SUPPORTED,
        /// Runs without a transaction; fails if there is one.
        NEVER,
        /// Runs in a nested transaction (savepoint) if there is a current one, otherwise like `REQUIRED`.
        NESTED
    }

    /// A named unit-of-work boundary, mapped to a `TransactionTemplate`.
    ///
    /// @param propagation the transaction propagation
    /// @param readOnly whether the transaction is read-only
    /// @param timeout the transaction timeout, in whole seconds; `null` for the transaction manager's default
    /// @param transactionManager bean name of the transaction manager; empty for the primary one
    public record BoundaryProperties(
            @DefaultValue("required") Propagation propagation,
            @DefaultValue("false") boolean readOnly,
            Duration timeout,
            String transactionManager) {}

    /// A named retry policy, turned into a [MifosFlow.RetryPolicy].
    ///
    /// @param maxAttempts total number of attempts, including the first one
    /// @param initialBackoff delay after the first failed attempt
    /// @param multiplier growth factor of the delay per failed attempt
    /// @param maxBackoff upper bound for the delay
    /// @param jitter fraction in `[0, 1]` by which each delay is randomly shortened
    /// @param retryOn fully qualified exception class names; empty means
    ///     `org.springframework.dao.ConcurrencyFailureException` (optimistic and pessimistic locking conflicts)
    public record RetryProperties(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("50ms") Duration initialBackoff,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("1s") Duration maxBackoff,
            @DefaultValue("0.5") double jitter,
            @DefaultValue List<String> retryOn) {}

    /// A named fork/join executor.
    ///
    /// @param virtualThreads whether forks run on virtual threads
    /// @param concurrencyLimit maximum number of concurrently running forks; -1 for unlimited
    public record ExecutorProperties(
            @DefaultValue("true") boolean virtualThreads,
            @DefaultValue("-1") int concurrencyLimit) {}
}
