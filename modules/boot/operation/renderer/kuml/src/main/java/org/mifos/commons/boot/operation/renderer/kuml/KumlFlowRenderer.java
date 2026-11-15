///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.renderer.kuml;

import dev.kuml.bpmn.dsl.BpmnModelBuilder;
import dev.kuml.bpmn.model.EventDefinition;
import dev.kuml.bpmn.model.GatewayType;
import dev.kuml.bpmn.model.ProcessDiagram;
import dev.kuml.bpmn.model.TaskType;
import dev.kuml.core.model.DiagramType;
import dev.kuml.core.model.KumlDiagram;
import dev.kuml.io.svg.KumlSvgRenderer;
import dev.kuml.io.svg.SvgRenderOptions;
import dev.kuml.layout.LayoutHints;
import dev.kuml.layout.bridge.bpmn.BpmnContentSizeProvider;
import dev.kuml.layout.bridge.bpmn.BpmnLayoutBridge;
import dev.kuml.layout.elk.ElkLayoutEngine;
import dev.kuml.renderer.theme.core.PlainThemeKt;
import java.util.Map;
import kotlin.Unit;
import org.mifos.commons.boot.operation.flow.MifosFlow;
import org.mifos.commons.boot.operation.flow.spi.Renderer;

public final class KumlFlowRenderer implements Renderer {
    @Override
    public String render(String title, MifosFlow.Definition<?> definition) {
        // 1. Build the BPMN model
        var mb = new BpmnModelBuilder("Order Management");

        mb.process("p_order", "Order Process", p -> {
            var start = p.startEvent("Order received", EventDefinition.MESSAGE);
            var check = p.task("Check stock", TaskType.SERVICE, t -> Unit.INSTANCE);
            var split = p.gateway(GatewayType.EXCLUSIVE, "In stock?", null);
            var ship = p.task("Ship order", TaskType.USER, t -> Unit.INSTANCE);
            var reord = p.task("Reorder stock", TaskType.SERVICE, t -> Unit.INSTANCE);
            var join = p.gateway(GatewayType.EXCLUSIVE, null, null);
            var end = p.endEvent("Order completed", EventDefinition.NONE);

            // sequenceFlow(from, to, condition, name, isDefault)
            p.sequenceFlow(start, check, null, null, false);
            p.sequenceFlow(check, split, null, null, false);
            p.sequenceFlow(split, ship, "stock > 0", "yes", false);
            p.sequenceFlow(split, reord, "stock == 0", "no", true);
            p.sequenceFlow(ship, join, null, null, false);
            p.sequenceFlow(reord, join, null, null, false);
            p.sequenceFlow(join, end, null, null, false);

            return Unit.INSTANCE;
        });

        mb.diagram("Order Process", "p_order", d -> Unit.INSTANCE);

        var model = mb.build();

        var view = (ProcessDiagram) model.getDiagrams().getFirst();
        var process = model.getProcesses().getFirst();

        // 2. Lay it out with ELK via the BPMN layout bridge
        var graph = BpmnLayoutBridge.INSTANCE.toLayoutGraph(model, view, new BpmnContentSizeProvider(model));
        var layout = new ElkLayoutEngine().layout(graph, LayoutHints.Companion.getDEFAULT());

        // 3. Wrap the process in a KumlDiagram and render the SVG
        var diagram = new KumlDiagram(
                view.getName(),
                DiagramType.BPMN_PROCESS,
                process.renderableElements(),
                view.getName(),
                Map.of(),
                null,
                null);

        return KumlSvgRenderer.INSTANCE.toSvg(
                diagram, layout, PlainThemeKt.PlainTheme(), SvgRenderOptions.Companion.getDEFAULT());
    }
}
