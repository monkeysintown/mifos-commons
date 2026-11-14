///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import org.mifos.commons.boot.operation.core.MifosOperationProperties;
import org.mifos.commons.boot.operation.core.flow.MifosFlow;
import org.mifos.commons.boot.operation.core.flow.engine.DefaultFlowEngine;
import org.mifos.commons.boot.operation.core.flow.spi.BoundaryResolver;
import org.mifos.commons.boot.operation.core.flow.spi.ExecutorResolver;
import org.mifos.commons.boot.operation.core.flow.spi.RetryPolicyResolver;
import org.mifos.commons.boot.operation.core.flow.spi.UsecaseResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.transaction.PlatformTransactionManager;

/// Provides a [Engine] assembled from replaceable parts.
///
/// Every part is a bean guarded by `@ConditionalOnMissingBean`, so an application can replace a single resolver,
/// add [Interceptor] beans (picked up in `@Order` order), adjust the builder with [EngineCustomizer] beans,
/// or replace the whole engine by declaring its own [Engine].
@AutoConfiguration(
        afterName = {
            // Spring Boot 3.x and 4.x locations; whichever is absent is ignored
            "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration",
            "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration"
        })
@EnableConfigurationProperties(MifosOperationProperties.class)
public class MifosOperationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    BoundaryResolver mifosFlowBoundaryResolver(
            MifosOperationProperties properties,
            ObjectProvider<PlatformTransactionManager> transactionManager,
            BeanFactory beanFactory) {
        return new TransactionTemplateBoundaryResolver(properties, transactionManager, beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    UsecaseResolver mifosFlowUsecaseResolver(BeanFactory beanFactory) {
        return new BeanFactoryUsecaseResolver(beanFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    RetryPolicyResolver mifosFlowRetryPolicyResolver(MifosOperationProperties properties) {
        return new PropertiesRetryPolicyResolver(properties, getClass().getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean
    ExecutorResolver mifosFlowExecutorResolver(
            MifosOperationProperties properties,
            @Qualifier("mifosFlowTaskDecorator") ObjectProvider<TaskDecorator> decorator) {
        return new PropertiesExecutorResolver(properties, decorator.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    MifosFlow.Engine mifosFlowEngine(
            BoundaryResolver boundaries,
            UsecaseResolver usecases,
            RetryPolicyResolver retryPolicies,
            ExecutorResolver executors,
            ObjectProvider<MifosFlow.Interceptor> interceptors,
            ObjectProvider<EngineCustomizer> customizers) {
        var builder = DefaultFlowEngine.builder()
                .boundaries(boundaries)
                .usecases(usecases)
                .retryPolicies(retryPolicies)
                .executors(executors);
        interceptors.orderedStream().forEach(builder::interceptor);
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.context.ContextSnapshotFactory")
    static class ContextPropagationConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "mifosFlowTaskDecorator")
        TaskDecorator mifosFlowTaskDecorator() {
            return new ContextPropagatingTaskDecorator();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.observation.ObservationRegistry")
    static class ObservationConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(ObservationRegistry.class)
        ObservationFlowInterceptor mifosFlowObservationInterceptor(ObservationRegistry registry) {
            return new ObservationFlowInterceptor(registry);
        }
    }
}
