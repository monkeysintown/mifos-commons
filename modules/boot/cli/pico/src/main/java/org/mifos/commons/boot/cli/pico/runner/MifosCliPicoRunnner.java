///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.pico.runner;

import static org.jline.utils.AttributedStyle.BLUE;
import static org.jline.utils.AttributedStyle.DEFAULT;
import static org.jline.utils.AttributedStyle.GREEN;
import static org.mifos.commons.boot.core.MifosConstants.MIFOS_SYMBOL_GLOBE;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStringBuilder;
import org.jspecify.annotations.NonNull;
import org.mifos.commons.boot.core.cli.MifosCliRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnMissingBean(value = MifosCliRunner.class, ignored = MifosCliPicoRunnner.class)
final class MifosCliPicoRunnner implements MifosCliRunner {
    private final CommandLine commandLine;
    private final LineReader reader;

    @Getter
    private int exitCode;

    @Override
    public void run(String @NonNull ... args) {
        try {
            if (args.length == 0) {
                loop();
            } else {
                exitCode = commandLine.execute(args);
            }
        } catch (Exception _) {
            exitCode = -1;
        }
    }

    private void loop() {
        while (true) {
            String line = reader.readLine(new AttributedStringBuilder()
                    .style(DEFAULT.foreground(GREEN).faint())
                    .append("mfg ")
                    .style(DEFAULT.bold().foreground(BLUE))
                    .append(MIFOS_SYMBOL_GLOBE)
                    .append(" ")
                    .toAnsi());

            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("q")) {
                break;
            }

            exitCode = commandLine.execute(line.split(" "));
        }
    }
}
