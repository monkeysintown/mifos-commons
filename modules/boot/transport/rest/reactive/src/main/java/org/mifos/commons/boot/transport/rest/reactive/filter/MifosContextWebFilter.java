///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.reactive.filter;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.transport.rest.core.MifosTransportRestProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/// A [org.springframework.web.server.WebFilter] that propagates selected HTTP request headers into the reactive
/// [reactor.util.context.Context] for downstream components.
///
/// This filter is designed for use in a Spring WebFlux application. It reads the incoming
/// [org.springframework.web.server.ServerWebExchange], extracts headers whose names are configured in
/// [MifosTransportRestProperties#getHeaders()], and stores their first value into the Reactor `MifosFlowContext`
/// under a lower‑cased key. This allows services (e.g., reactive REST clients) to access those headers without
/// explicitly passing them through method calls.
///
/// The filter uses [reactor.core.publisher.Mono#contextWrite(java.util.function.Function)] to add the header
/// entries to the context that will be visible to operators downstream of the current filter chain.
///
/// **Note:** Only headers that are listed in the properties `headers` collection, have a non‑`null`
/// value, and a non‑empty list of values will be propagated. The first value of a multi‑value header is taken.
///
/// @see MifosTransportRestProperties
/// @see org.springframework.web.server.WebFilter
/// @see reactor.util.context.Context
@Slf4j
@RequiredArgsConstructor
@Component
public class MifosContextWebFilter implements WebFilter {
    private final MifosTransportRestProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // NOTE: see
        // https://stackoverflow.com/questions/55614619/using-webclient-to-propagate-request-headers-received-in-a-spring-webflux-applic

        return chain.filter(exchange).contextWrite(context -> {
            exchange.getRequest().getHeaders().forEach((key, value) -> {
                if (properties.getHeaders().contains(key.toLowerCase(Locale.ROOT)) && !value.isEmpty()) {
                    context.put(key.toLowerCase(Locale.ROOT), value.getFirst());
                }
            });

            return context;
        });
    }
}
