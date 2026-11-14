///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.renderer.plantuml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class PlantUmlFlowRendererTest {
    @Test
    @Disabled
    @SuppressWarnings({"java:S1607", "java:S1135"})
    void render() throws Exception {
        var renderer = new PlantUmlFlowRenderer();

        // TODO: fix this
        var puml = renderer.render("Test", null);

        assertNotNull(puml);

        FileUtils.writeStringToFile(new File("build/flow.puml"), puml, UTF_8);
    }
}
