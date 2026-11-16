///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.renderer.kuml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

final class KumlFlowRendererTest {
    @Test
    @SuppressWarnings({"java:S1607", "java:S1135"})
    void render() throws Exception {
        var renderer = new KumlFlowRenderer();

        // TODO: fix this
        var svg = renderer.render(null, "Test");

        assertNotNull(svg);

        FileUtils.writeStringToFile(new File("build/flow.svg"), svg, UTF_8);
    }
}
