///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.core.autoconfigure;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_HEADER_TENANT_ID;
import static org.mifos.commons.boot.transport.rest.core.MifosTransportRestConstants.MIFOS_COMMONS_BOOT_TRANSPORT_REST_CORE_PACKAGE;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.transport.rest.core.MifosTransportRestProperties;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.HandlerMethod;

/// Spring configuration class for setting up REST transport components in the Mifos Initiative.
///
/// This configuration provides beans for the default page request, OpenAPI documentation, the tenant ID header in
/// OpenAPI operations, and CORS policies.
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
/// @see MifosTransportRestProperties
/// @see Configuration
@Configuration
@EnableConfigurationProperties({MifosTransportRestProperties.class})
@ComponentScan(MIFOS_COMMONS_BOOT_TRANSPORT_REST_CORE_PACKAGE)
@RequiredArgsConstructor
class MifosTransportRestAutoConfiguration {
    // TODO: fix modules (UnitJacksonModule, MoneyModule, JtsModule, ShapesAsGeoJSONModule)
    @Value("${application.version:unknown}")
    private String appVersion;

    @Value("${application.title:}")
    private String appTitle;

    @Bean
    PageRequest defaultPageRequest(MifosTransportRestProperties properties) {
        return PageRequest.of(properties.getDefaultPageNumber(), properties.getDefaultPageSize());
    }

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mifos Initiative REST API: " + appTitle)
                        .description("Mifos Initiative REST API: " + appTitle)
                        .version(appVersion)
                        .contact(new Contact()))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Mifos Initiative '%s' Actuator (Local)".formatted(appTitle)),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Mifos Initiative '%s' API (Local)".formatted(appTitle)),
                        new Server()
                                .url("https://demo.mifos.org")
                                .description("Mifos Initiative '%s' API (Demo)".formatted(appTitle))))
                .components(new Components()
                        .addSecuritySchemes(
                                "bearer-key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    OperationCustomizer tenantIdHeader() {
        return (Operation operation, HandlerMethod _) -> {
            var tenantIdParam = new Parameter()
                    .in(ParameterIn.HEADER.toString())
                    .schema(new StringSchema())
                    .name(MIFOS_COMMONS_BOOT_HEADER_TENANT_ID)
                    .description("Tenant ID")
                    .required(true)
                    .example("default");

            operation.addParametersItem(tenantIdParam);

            return operation;
        };
    }

    @Bean
    CorsConfigurationSource cors() {
        // TODO: improve this, maybe do in application.yml
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
