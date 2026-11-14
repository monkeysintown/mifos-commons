///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.tamboui.runner;

import dev.tamboui.toolkit.app.ToolkitApp;
import lombok.RequiredArgsConstructor;
import org.mifos.commons.boot.cli.core.MifosCliRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@ConditionalOnMissingBean(value = MifosCliRunner.class, ignored = MifosCliTambouiRunner.class)
final class MifosCliTambouiRunner implements MifosCliRunner {
    private final ToolkitApp app;
    private int exitCode;

    @Override
    public void run(String... args) throws Exception {
        app.run();
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
