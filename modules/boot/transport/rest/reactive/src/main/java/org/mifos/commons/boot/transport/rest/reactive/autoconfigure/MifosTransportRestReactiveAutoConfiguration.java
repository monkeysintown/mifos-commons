///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.reactive.autoconfigure;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;
import static java.util.Locale.GERMAN;
import static org.mifos.commons.boot.transport.rest.core.MifosTransportRestConstants.MIFOS_COMMONS_BOOT_TRANSPORT_REST_REACTIVE_PACKAGE;

import java.util.Arrays;
import java.util.Locale;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver;
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

/// Spring configuration class for setting up reactive (WebFlux) REST transport components in the Mifos Initiative.
///
/// This configuration provides beans for `Accept-Language` header based locale resolution and pageable/sort
/// argument handling (falling back to the default page request).
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
/// @see org.mifos.commons.boot.transport.rest.core.MifosTransportRestProperties
/// @see org.springframework.context.annotation.Configuration
@Configuration
@ComponentScan(MIFOS_COMMONS_BOOT_TRANSPORT_REST_REACTIVE_PACKAGE)
class MifosTransportRestReactiveAutoConfiguration {
    // TODO: fix modules (UnitJacksonModule, MoneyModule, JtsModule, ShapesAsGeoJSONModule)

    @Bean
    WebFluxConfigurer springDataArgumentResolversConfigurer(PageRequest pageRequest) {
        return new WebFluxConfigurer() {
            @Override
            public void configureArgumentResolvers(@NonNull ArgumentResolverConfigurer configurer) {
                var argumentResolver = new ReactiveSortHandlerMethodArgumentResolver();

                var resolver = new ReactivePageableHandlerMethodArgumentResolver(argumentResolver);
                resolver.setFallbackPageable(pageRequest);

                configurer.addCustomResolver(argumentResolver, resolver);
            }
        };
    }

    @Bean
    LocaleContextResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleContextResolver();
        resolver.setDefaultLocale(ENGLISH);
        resolver.setSupportedLocales(
                Arrays.asList(ENGLISH, FRENCH, GERMAN, Locale.of("es"), Locale.of("es"), Locale.of("in")));

        return resolver;
    }
}
