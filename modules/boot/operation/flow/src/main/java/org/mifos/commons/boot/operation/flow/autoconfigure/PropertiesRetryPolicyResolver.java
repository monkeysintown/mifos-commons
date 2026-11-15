///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.autoconfigure;

import static org.mifos.commons.boot.operation.flow.exception.MifosFlowException.MifosFlowErrorCode.MIFOS_COMMONS_ERROR_FLOW_CLASS_UNKNOWN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.operation.flow.MifosFlow.RetryPolicy;
import org.mifos.commons.boot.operation.flow.MifosFlowProperties;
import org.mifos.commons.boot.operation.flow.exception.MifosFlowException;
import org.mifos.commons.boot.operation.flow.spi.RetryPolicyResolver;
import org.springframework.util.ClassUtils;

/// Resolves named retry policies from `mifos.flow.retry.*`. Exception class names are checked eagerly, so a typo
/// fails the application at startup.
final class PropertiesRetryPolicyResolver implements RetryPolicyResolver {
    static final String DEFAULT_RETRY_ON = "org.springframework.dao.ConcurrencyFailureException";

    private final Map<String, RetryPolicy> policies = new HashMap<>();

    public PropertiesRetryPolicyResolver(MifosFlowProperties properties, ClassLoader classLoader) {
        var configured = new HashMap<String, MifosFlowProperties.RetryProperties>();
        configured.put(
                "conflict",
                new MifosFlowProperties.RetryProperties(
                        3, java.time.Duration.ofMillis(50), 2.0, java.time.Duration.ofSeconds(1), 0.5, List.of()));
        configured.putAll(properties.retry());
        configured.forEach((name, props) -> policies.put(name, toPolicy(name, props, classLoader)));
    }

    @Override
    public Optional<RetryPolicy> resolve(String name) {
        return Optional.ofNullable(policies.get(name));
    }

    private static RetryPolicy toPolicy(String name, MifosFlowProperties.RetryProperties props, ClassLoader loader) {
        var names = props.retryOn().isEmpty() ? List.of(DEFAULT_RETRY_ON) : props.retryOn();
        var types = new ArrayList<Class<?>>();

        for (String className : names) {
            Class<?> type;

            try {
                type = ClassUtils.forName(className, loader);
            } catch (ClassNotFoundException | LinkageError e) {
                throw new MifosFlowException(
                        MifosError.of(MIFOS_COMMONS_ERROR_FLOW_CLASS_UNKNOWN, e, List.of(className)));
            }

            // TODO: decide how to solve this, maybe with a marker interface or with additional attributes on MifosError
            if (!RuntimeException.class.isAssignableFrom(type)) {
                throw new IllegalStateException(
                        "mifos.flow.retry." + name + ".retry-on: " + className + " is not a RuntimeException");
            }

            types.add(type);
        }

        Predicate<RuntimeException> retryable = e -> types.stream().anyMatch(t -> t.isInstance(e));

        return RetryPolicy.on(props.maxAttempts(), retryable)
                .withBackoff(props.initialBackoff(), props.multiplier(), props.maxBackoff())
                .withJitter(props.jitter());
    }
}
