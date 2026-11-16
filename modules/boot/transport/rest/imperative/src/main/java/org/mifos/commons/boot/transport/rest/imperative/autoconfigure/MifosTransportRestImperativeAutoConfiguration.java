///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.imperative.autoconfigure;

import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRENCH;
import static java.util.Locale.GERMAN;
import static org.mifos.commons.boot.transport.rest.core.MifosTransportRestConstants.MIFOS_COMMONS_BOOT_TRANSPORT_REST_IMPERATIVE_PACKAGE;

import java.util.Arrays;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/// Spring configuration class for setting up imperative (Servlet) REST transport components in the Mifos Initiative.
///
/// This configuration provides the primary `Accept-Language` header based locale resolver.
///
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
/// @see org.mifos.commons.boot.transport.rest.core.MifosTransportRestProperties
/// @see Configuration
@Configuration
@ComponentScan(MIFOS_COMMONS_BOOT_TRANSPORT_REST_IMPERATIVE_PACKAGE)
class MifosTransportRestImperativeAutoConfiguration {
    // TODO: fix modules (UnitJacksonModule, MoneyModule, JtsModule, ShapesAsGeoJSONModule)

    @Primary
    @Bean
    LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(ENGLISH);
        resolver.setSupportedLocales(
                Arrays.asList(ENGLISH, FRENCH, GERMAN, Locale.of("es"), Locale.of("es"), Locale.of("in")));
        return resolver;
    }
}
