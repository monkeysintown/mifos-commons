///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.reactive.autoconfigure;

import static org.mifos.commons.boot.core.MifosConstants.MIFOS_PACKAGE_BASE;

import com.infobip.spring.data.r2dbc.EnableQuerydslR2dbcRepositories;
import com.infobip.spring.data.r2dbc.QuerydslR2dbcRepositoryFactoryBean;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spatial.PostGISTemplates;
import com.querydsl.sql.spring.SpringExceptionTranslator;
import com.querydsl.sql.types.EnumByNameType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.persistence.relational.core.MifosPersistenceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/// Auto-configuration of reactive relational persistence: Spring Data R2DBC repositories with QueryDSL support,
/// R2DBC auditing, transaction management and the QueryDSL SQL configuration (PostGIS templates).
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties({MifosPersistenceProperties.class})
@EnableQuerydslR2dbcRepositories
@EnableR2dbcRepositories(
        basePackages = MIFOS_PACKAGE_BASE,
        repositoryFactoryBeanClass = QuerydslR2dbcRepositoryFactoryBean.class)
@EnableR2dbcAuditing
@EnableTransactionManagement
public class MifosPersistenceRelationalReactiveAutoConfiguration {
    private final MifosPersistenceProperties properties;

    @Bean
    SQLTemplates sqlTemplates() {
        // TODO: figure out auto detection
        return PostGISTemplates.builder().build();
    }

    @Bean
    @Primary
    com.querydsl.sql.Configuration querydslSqlConfiguration(
            SQLTemplates sqlTemplates, List<EnumByNameType<?>> enumsByNameType) {
        var configuration = new com.querydsl.sql.Configuration(sqlTemplates);
        configuration.setExceptionTranslator(new SpringExceptionTranslator());

        enumsByNameType.forEach(configuration::register);

        return configuration;
    }
}
