///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.autoconfigure;

import static org.mifos.commons.boot.operation.core.MifosOperationProperties.Propagation.REQUIRED;
import static org.mifos.commons.boot.operation.core.MifosOperationProperties.Propagation.REQUIRES_NEW;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.mifos.commons.boot.operation.core.MifosOperationProperties;
import org.mifos.commons.boot.operation.core.flow.spi.Boundary;
import org.mifos.commons.boot.operation.core.flow.spi.BoundaryResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/// Resolves named boundaries to `TransactionTemplate`s.
///
/// Transaction managers are looked up lazily, when a flow is bound, so this works regardless of auto-configuration
/// order and in applications with several transaction managers.
public final class TransactionTemplateBoundaryResolver implements BoundaryResolver {
    private final Map<String, MifosOperationProperties.BoundaryProperties> definitions;
    private final ObjectProvider<PlatformTransactionManager> primary;
    private final BeanFactory beanFactory;
    private final Map<String, Boundary> cache = new ConcurrentHashMap<>();

    public TransactionTemplateBoundaryResolver(
            MifosOperationProperties properties,
            ObjectProvider<PlatformTransactionManager> primary,
            BeanFactory beanFactory) {
        this.definitions = new HashMap<>();
        definitions.put("required", props(REQUIRED, false));
        definitions.put("requires-new", props(REQUIRES_NEW, false));
        definitions.put("read-only", props(REQUIRED, true));
        definitions.putAll(properties.boundaries());
        this.primary = primary;
        this.beanFactory = beanFactory;
    }

    @Override
    public Optional<Boundary> resolve(String name) {
        var definition = definitions.get(name);
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(cache.computeIfAbsent(name, n -> boundary(template(definition))));
    }

    private TransactionTemplate template(MifosOperationProperties.BoundaryProperties definition) {
        PlatformTransactionManager manager = definition.transactionManager() == null
                        || definition.transactionManager().isBlank()
                ? primary.getObject()
                : beanFactory.getBean(definition.transactionManager(), PlatformTransactionManager.class);
        var template = new TransactionTemplate(manager);
        template.setPropagationBehavior(
                switch (definition.propagation()) {
                    case REQUIRED -> TransactionDefinition.PROPAGATION_REQUIRED;
                    case REQUIRES_NEW -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
                    case MANDATORY -> TransactionDefinition.PROPAGATION_MANDATORY;
                    case SUPPORTS -> TransactionDefinition.PROPAGATION_SUPPORTS;
                    case NOT_SUPPORTED -> TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
                    case NEVER -> TransactionDefinition.PROPAGATION_NEVER;
                    case NESTED -> TransactionDefinition.PROPAGATION_NESTED;
                });
        template.setReadOnly(definition.readOnly());
        if (definition.timeout() != null) {
            template.setTimeout((int) definition.timeout().toSeconds());
        }
        return template;
    }

    private static Boundary boundary(TransactionTemplate template) {
        return new Boundary() {
            @Override
            public <T> T run(Supplier<T> work) {
                return template.execute(status -> work.get());
            }
        };
    }

    private static MifosOperationProperties.BoundaryProperties props(
            MifosOperationProperties.Propagation propagation, boolean readOnly) {
        return new MifosOperationProperties.BoundaryProperties(propagation, readOnly, null, null);
    }
}
