///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import org.mifos.commons.boot.operation.flow.MifosFlowProperties;
import org.mifos.commons.boot.operation.flow.spi.ExecutorResolver;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;

/// Resolves fork/join executors from `mifos.flow.executors.*`; `default` always exists.
///
/// Every executor gets the given [TaskDecorator]. With Micrometer's context-propagation library on the classpath,
/// the auto-configuration passes a `ContextPropagatingTaskDecorator`, so MDC, tracing and security context reach
/// the forks.
final class PropertiesExecutorResolver implements ExecutorResolver, DisposableBean {
    private final Map<String, MifosFlowProperties.ExecutorProperties> definitions = new HashMap<>();
    private final TaskDecorator decorator;
    private final Map<String, SimpleAsyncTaskExecutor> executors = new ConcurrentHashMap<>();

    public PropertiesExecutorResolver(MifosFlowProperties properties, TaskDecorator decorator) {
        definitions.put("default", new MifosFlowProperties.ExecutorProperties(true, -1));
        definitions.putAll(properties.executors());

        this.decorator = decorator;
    }

    @Override
    public Optional<Executor> resolve(String name) {
        var definition = definitions.get(name);

        if (definition == null) {
            return Optional.empty();
        }

        return Optional.of(executors.computeIfAbsent(name, n -> create(n, definition)));
    }

    private SimpleAsyncTaskExecutor create(String name, MifosFlowProperties.ExecutorProperties definition) {
        var executor = new SimpleAsyncTaskExecutor("mifos-flow-%s-".formatted(name));
        executor.setVirtualThreads(definition.virtualThreads());
        executor.setConcurrencyLimit(definition.concurrencyLimit());
        executor.setTaskTerminationTimeout(10_000); // lets close() wait for running forks on shutdown

        if (decorator != null) {
            executor.setTaskDecorator(decorator);
        }

        return executor;
    }

    @Override
    public void destroy() {
        executors.values().forEach(SimpleAsyncTaskExecutor::close);
    }
}
