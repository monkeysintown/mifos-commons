///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.imperative.autoconfigure;

import com.infobip.spring.data.jdbc.EnableQuerydslJdbcRepositories;
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
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/// Auto-configuration of imperative relational persistence: Spring Data JDBC repositories with QueryDSL support,
/// JDBC auditing, transaction management and the QueryDSL SQL configuration (PostGIS templates).
@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties({MifosPersistenceProperties.class})
@EnableQuerydslJdbcRepositories
@EnableJdbcAuditing
@EnableTransactionManagement
public class MifosPersistenceRelationalImperativeAutoConfiguration {
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
