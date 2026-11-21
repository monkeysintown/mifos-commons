///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.starter.usecase;

import org.mifos.commons.boot.operation.core.usecase.spi.UsecaseResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MifosUsecaseConfiguration {
    @Bean
    @ConditionalOnMissingBean
    UsecaseResolver mifosFlowUsecaseResolver(BeanFactory beanFactory) {
        return new BeanFactoryUsecaseResolver(beanFactory);
    }
}
