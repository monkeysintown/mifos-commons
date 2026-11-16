///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.application.banner;

import static org.mifos.commons.boot.core.MifosConstants.MIFOS_PROPERTY_BANNER;

import java.io.PrintStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.env.Environment;

/// Startup banner rendering "Mifos - <text>" (text taken from the `mifos.banner` property) as FIGlet ASCII art.
@Slf4j
@RequiredArgsConstructor
@ImportRuntimeHints(MifosBanner.MifosBannerRuntimeHints.class)
final class MifosBanner implements Banner {
    private static final String SUBTITLE = "Brought to you by The Mifos Initiative - (c) 2026 - https://mifos.org";
    private static final String FONT_SLANT_RESOURCE = "fonts/slant.flf";

    private static final int STRAP_LINE_SIZE = 42;

    @Override
    public void printBanner(Environment environment, @Nullable Class<?> sourceClass, PrintStream out) {
        try (var font = MifosBanner.class.getClassLoader().getResourceAsStream(FONT_SLANT_RESOURCE)) {
            var text = environment.getProperty(MIFOS_PROPERTY_BANNER, String.class);
            var banner =
                    Figlet.renderTracked(font, "Mifos - %s".formatted(text)).banner();
            var version = String.format("--- Powered by Spring Boot (v%s) ---", SpringBootVersion.getVersion());
            var padding = " ".repeat(Math.max(0, STRAP_LINE_SIZE - (version.length() + banner.length())));

            out.println();
            out.println(AnsiOutput.toString(AnsiColor.BRIGHT_GREEN, banner));
            out.println();
            out.println(AnsiOutput.toString(AnsiColor.DEFAULT, padding, AnsiStyle.FAINT, SUBTITLE));
            out.println();
            out.println(AnsiOutput.toString(AnsiColor.DEFAULT, padding, AnsiStyle.FAINT, version));
            out.println();
        } catch (Exception e) {
            log.error("Problem loading banner font: ", e);
        }
    }

    /// Registers the banner font resource for native images.
    static class MifosBannerRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.resources().registerPattern(FONT_SLANT_RESOURCE);
        }
    }
}
