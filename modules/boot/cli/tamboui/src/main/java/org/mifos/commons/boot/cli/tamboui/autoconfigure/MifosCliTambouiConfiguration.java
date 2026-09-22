///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.tamboui.autoconfigure;

import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.error.RenderErrorHandler;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MifosCliTambouiConfiguration {
    @Bean
    TuiConfig tuiConfig(RenderErrorHandler errorHandler) {
        return TuiConfig.defaults().toBuilder()
                // .errorHandler(errorHandler)
                .mouseCapture(true)
                .bindings(BindingSets.intellij())
                .tickRate(Duration.ofMillis(16))
                .build();
    }
}
