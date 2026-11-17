///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.renderer.kuml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.act;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.compensable;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.forEach;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.forkJoin;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.named;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.noop;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.repeatWhile;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.saga;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.sequence;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.tap;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.transactional;
import static org.mifos.commons.boot.operation.flow.MifosFlow.Dsl.when;

import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mifos.commons.boot.operation.flow.MifosFlow.Definition;
import org.mifos.commons.boot.operation.flow.MifosFlow.MifosFlowContext;
import org.mifos.commons.boot.operation.flow.spi.BoundaryRef;
import org.mifos.commons.boot.operation.flow.spi.RetryRef;

final class KumlFlowRendererTest {
    private final KumlFlowRenderer renderer = new KumlFlowRenderer();

    record Ctx(int value) implements MifosFlowContext {}

    static Definition<Ctx> action(String name) {
        return named(name, act(c -> c));
    }

    @Test
    void rendersEveryKindOfNode() throws Exception {
        Definition<Ctx> transfer = named(
                "transfer",
                sequence(
                        action("validate"),
                        when((Ctx c) -> c.value() > 0, action("charge fee"), action("waive fee")),
                        forkJoin(c -> c, List.of(action("score risk"), action("check limits")), (Ctx a, Ctx b) -> b),
                        transactional(
                                BoundaryRef.REQUIRED,
                                RetryRef.CONFLICT,
                                sequence(action("post entry"), action("update balance"))),
                        repeatWhile((Ctx c) -> false, 3, action("poll status")),
                        forEach((Ctx c) -> List.of(1, 2), (Ctx c, Integer i) -> c),
                        saga(
                                compensable(action("reserve funds"), action("release funds")),
                                compensable(action("book transfer"), noop())),
                        named("notify", tap((Ctx c) -> {}))));

        var svg = renderer.render(transfer);
        FileUtils.writeStringToFile(new File("build/flow.svg"), svg, UTF_8);

        assertTrue(svg.contains("<svg"), "not an SVG document");
        for (var label : List.of("validate", "within", "saga", "reserve")) {
            assertTrue(svg.contains(label), () -> "missing label: " + label);
        }
    }

    @Test
    void rendersAnEmptyFlowWithoutTitle() {
        var svg = renderer.render(noop(), null);

        assertTrue(svg.contains("<svg"), "not an SVG document");
    }

    @Test
    void rejectsMissingDefinition() {
        assertThrows(NullPointerException.class, () -> renderer.render(null, "Test"));
    }
}
