///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.shell.runner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.cli.core.MifosCliRunner;
import org.springframework.shell.core.ShellRunner;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
final class MifosCliShellRunnner implements MifosCliRunner {
    private final ShellRunner shellRunner;

    @Getter
    private int exitCode;

    @Override
    public void run(String... args) throws Exception {
        shellRunner.run(args);
    }
}
