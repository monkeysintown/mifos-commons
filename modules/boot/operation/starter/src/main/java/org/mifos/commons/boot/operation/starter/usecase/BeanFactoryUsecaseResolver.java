///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.starter.usecase;

import java.util.Optional;
import org.mifos.commons.boot.operation.core.usecase.spi.UsecaseResolver;
import org.springframework.beans.factory.BeanFactory;

/// Resolves usecases referenced by type to the unique bean of that type.
final class BeanFactoryUsecaseResolver implements UsecaseResolver {
    private final BeanFactory beanFactory;

    public BeanFactoryUsecaseResolver(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public <U> Optional<U> resolve(Class<U> type) {
        return Optional.ofNullable(beanFactory.getBeanProvider(type).getIfUnique());
    }
}
