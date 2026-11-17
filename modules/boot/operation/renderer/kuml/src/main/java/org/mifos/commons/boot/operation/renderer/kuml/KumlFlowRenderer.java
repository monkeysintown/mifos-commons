///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.renderer.kuml;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.Var;
import dev.kuml.bpmn.dsl.BpmnModelBuilder;
import dev.kuml.bpmn.dsl.ProcessBuilder;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import kotlin.Unit;
import org.mifos.commons.boot.operation.flow.MifosFlow;
import org.mifos.commons.boot.operation.flow.spi.ExecutorRef;
import org.mifos.commons.boot.operation.flow.spi.Renderer;

/// Renders a [MifosFlow.Definition] as a BPMN process diagram in SVG format using kUML (laid out with ELK).
///
/// Only the definition is needed, not a bound flow, so diagrams can be generated in a unit test, a build step or an
/// actuator endpoint without any usecase, transaction or executor being available. The mapping:
///
/// | Definition                     | BPMN                                                                         |
/// |--------------------------------|------------------------------------------------------------------------------|
/// | `step`                         | service task, labelled with the usecase's name                               |
/// | `check`                        | business rule task, labelled with the usecase's name                         |
/// | `act`                          | script task                                                                  |
/// | `tap`                          | throwing intermediate signal event                                           |
/// | `named` around a single action | that action, labelled with the name                                          |
/// | `named` around anything else   | expanded sub-process                                                         |
/// | `when`                         | exclusive split (`yes` / `no`) and join                                      |
/// | `repeatWhile`                  | exclusive gateway loop                                                       |
/// | `forEach`                      | sequential multi-instance task for a single action, gateway loop otherwise   |
/// | `forkJoin`                     | parallel split and join                                                      |
/// | `within`                       | transactional (double-framed) expanded sub-process                           |
/// | `retryOn`                      | interrupting error boundary event looping back to the guarded activity       |
/// | `recover`                      | interrupting error boundary event leading to a `recover` script task         |
/// | `saga`, `subflow`              | expanded sub-process; saga steps name their undo step                        |
///
/// kUML lays out one level of expanded sub-processes and attaches boundary events only at the top level of a
/// process. Groups nested inside a sub-process are therefore drawn inline, and `retryOn` / `recover` inside a
/// sub-process (or around more than one activity) become an exclusive gateway after the guarded part instead of a
/// boundary event.
///
/// Conditions, loop items and anonymous `act` steps are lambdas, so the diagram cannot describe them; wrap them in
/// `named` where the diagram is meant for readers.
public final class KumlFlowRenderer implements Renderer {
    @Override
    public String render(MifosFlow.Definition<?> definition, String title) {
        requireNonNull(definition, "definition");

        var name = title == null || title.isBlank() ? DEFAULT_TITLE : title;
        var mb = new BpmnModelBuilder(name);

        mb.process(DEFAULT_PROCESS_ID, name, p -> {
            new Compiler(p, 0).wrap(c -> c.node(definition, null));
            return Unit.INSTANCE;
        });
        mb.diagram(name, DEFAULT_PROCESS_ID, d -> Unit.INSTANCE);

        var model = mb.build();
        var view = (ProcessDiagram) model.getDiagrams().getFirst();
        var process = model.getProcesses().getFirst();

        var graph = BpmnLayoutBridge.INSTANCE.toLayoutGraph(model, view, new BpmnContentSizeProvider(model));
        var layout = new ElkLayoutEngine().layout(graph, LayoutHints.Companion.getDEFAULT());

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

    /// A connected part of the diagram: flows enter at `in` and leave at `out`; `outLabel` names the outgoing flow
    /// (e.g. the exit branch of a loop), or is `null`.
    private record Fragment(String in, String out, String outLabel) {
        static Fragment of(String id) {
            return new Fragment(id, id, null);
        }

        boolean isSingleNode() {
            return in.equals(out) && outLabel == null;
        }
    }

    /// Translates definitions into the nodes and flows of one (sub-)process.
    private static final class Compiler {
        private final ProcessBuilder p;
        private final int depth;
        private final Set<String> activities = new HashSet<>();

        Compiler(ProcessBuilder p, int depth) {
            this.p = p;
            this.depth = depth;
        }

        /// Adds start event → `content` → end event.
        void wrap(Function<Compiler, Fragment> content) {
            var start = p.startEvent(null, EventDefinition.NONE);
            var body = content.apply(this);
            var end = p.endEvent(null, EventDefinition.NONE);

            if (body == null) {
                flow(start, end, null, false);
            } else {
                flow(start, body.in(), null, false);
                connect(body, end);
            }
        }

        /// @param name the name of an enclosing [MifosFlow.Definition.Named] to use as label for a single action, or
        /// `null`
        /// @return the fragment, or `null` if the definition draws nothing
        Fragment node(MifosFlow.Definition<?> definition, String name) {
            return switch (definition) {
                case MifosFlow.Definition.Noop<?> _ -> name == null ? null : task(name, TaskType.NONE, false);
                case MifosFlow.Definition.Act<?> _ -> task(label(definition, name), TaskType.SCRIPT, false);
                case MifosFlow.Definition.Step<?, ?, ?> _ -> task(label(definition, name), TaskType.SERVICE, false);
                case MifosFlow.Definition.Check<?, ?> _ -> task(label(definition, name), TaskType.BUSINESS_RULE, false);
                case MifosFlow.Definition.Tap<?> _ ->
                    Fragment.of(p.intermediateEvent(label(definition, name), EventDefinition.SIGNAL, true));
                case MifosFlow.Definition.Named<?> named ->
                    isSingleAction(named.body())
                            ? node(named.body(), named.name())
                            : group(named.name(), false, c -> c.node(named.body(), null));
                case MifosFlow.Definition.Sequence<?> sequence -> sequence(sequence.steps());
                case MifosFlow.Definition.When<?> when -> when(when);
                case MifosFlow.Definition.RepeatWhile<?> repeat ->
                    loop("while condition? (at most " + repeat.maxIterations() + " times)", "yes", "no", repeat.body());
                case MifosFlow.Definition.ForEach<?, ?> forEach -> forEach(forEach);
                case MifosFlow.Definition.Subflow<?, ?> subflow ->
                    group("subflow", false, c -> c.node(subflow.body(), null));
                case MifosFlow.Definition.ForkJoin<?> forkJoin -> forkJoin(forkJoin);
                case MifosFlow.Definition.Within<?> within ->
                    within.boundary().isNone()
                            ? node(within.body(), null)
                            : group("within " + within.boundary(), true, c -> c.node(within.body(), null));
                case MifosFlow.Definition.Retry<?> retry -> retry(retry);
                case MifosFlow.Definition.Recover<?> recover -> recover(recover);
                case MifosFlow.Definition.Saga<?> saga -> group("saga", false, c -> c.saga(saga));
            };
        }

        // --- composites ---

        private Fragment sequence(List<? extends MifosFlow.Definition<?>> steps) {
            @Var Fragment first = null;
            @Var Fragment last = null;

            for (MifosFlow.Definition<?> step : steps) {
                var fragment = node(step, null);
                if (fragment == null) {
                    continue;
                }
                if (last == null) {
                    first = fragment;
                } else {
                    connect(last, fragment.in());
                }
                last = fragment;
            }

            return first == null ? null : new Fragment(first.in(), last.out(), last.outLabel());
        }

        private Fragment when(MifosFlow.Definition.When<?> when) {
            var split = p.gateway(GatewayType.EXCLUSIVE, "condition?", null);
            var join = p.gateway(GatewayType.EXCLUSIVE, null, null);

            branch(split, when.then(), "yes", false, join);
            branch(split, when.otherwise(), "no", true, join);

            return new Fragment(split, join, null);
        }

        /// A gateway loop: `merge` → `test` → (`continueLabel`) `body` → `merge`; the loop is left with `exitLabel`.
        private Fragment loop(String question, String continueLabel, String exitLabel, MifosFlow.Definition<?> body) {
            var merge = p.gateway(GatewayType.EXCLUSIVE, null, null);
            var test = p.gateway(GatewayType.EXCLUSIVE, question, null);

            flow(merge, test, null, false);
            branch(test, body, continueLabel, false, merge);

            return new Fragment(merge, test, exitLabel);
        }

        private Fragment forEach(MifosFlow.Definition.ForEach<?, ?> forEach) {
            var body = forEach.body();

            return switch (body) {
                case MifosFlow.Definition.Noop<?> _ -> task("fold each item", TaskType.SCRIPT, false);
                case MifosFlow.Definition.Act<?> _ -> task(label(body, null), TaskType.SCRIPT, true);
                case MifosFlow.Definition.Step<?, ?, ?> _ -> task(label(body, null), TaskType.SERVICE, true);
                case MifosFlow.Definition.Check<?, ?> _ -> task(label(body, null), TaskType.BUSINESS_RULE, true);
                default -> loop("next item?", "yes", "done", body);
            };
        }

        private Fragment forkJoin(MifosFlow.Definition.ForkJoin<?> forkJoin) {
            if (forkJoin.forks().isEmpty()) {
                return null;
            }

            var executor =
                    forkJoin.executor().equals(ExecutorRef.DEFAULT) ? "merge" : "merge on " + forkJoin.executor();
            var split = p.gateway(GatewayType.PARALLEL, null, null);
            var join = p.gateway(GatewayType.PARALLEL, executor, null);

            for (MifosFlow.Definition<?> fork : forkJoin.forks()) {
                branch(split, fork, null, false, join);
            }

            return new Fragment(split, join, null);
        }

        private Fragment retry(MifosFlow.Definition.Retry<?> retry) {
            var body = node(retry.body(), null);
            if (body == null) {
                return null;
            }

            var label = "retryable failure (policy " + retry.policy() + ")";
            var merge = p.gateway(GatewayType.EXCLUSIVE, null, null);
            flow(merge, body.in(), null, false);

            if (canAttachBoundaryEvent(body)) {
                var failure = p.boundaryEvent(body.in(), label, EventDefinition.ERROR, true);
                flow(failure, merge, "retry", false);
                return new Fragment(merge, body.out(), null);
            }

            var test = p.gateway(GatewayType.EXCLUSIVE, label + "?", null);
            connect(body, test);
            flow(test, merge, "retry", false);
            return new Fragment(merge, test, "done");
        }

        private Fragment recover(MifosFlow.Definition.Recover<?> recover) {
            var body = depth == 0 && !isSingleActivity(recover.body())
                    ? group("recover expected failures", false, c -> c.node(recover.body(), null))
                    : node(recover.body(), null);
            if (body == null) {
                return null;
            }

            var join = p.gateway(GatewayType.EXCLUSIVE, null, null);
            var handler = task("recover", TaskType.SCRIPT, false);
            flow(handler.in(), join, null, false);

            if (canAttachBoundaryEvent(body)) {
                var failure = p.boundaryEvent(body.in(), "expected failure", EventDefinition.ERROR, true);
                connect(body, join);
                flow(failure, handler.in(), null, false);
                return new Fragment(body.in(), join, null);
            }

            // Inside a sub-process: no boundary events, so the recovery becomes a decision after the body.
            var test = p.gateway(GatewayType.EXCLUSIVE, "expected failure?", null);
            connect(body, test);
            flow(test, handler.in(), "yes", false);
            flow(test, join, "no", true);
            return new Fragment(body.in(), join, null);
        }

        private Fragment saga(MifosFlow.Definition.Saga<?> saga) {
            @Var Fragment first = null;
            @Var Fragment last = null;

            for (MifosFlow.Definition.Compensable<?> step : saga.steps()) {
                var undo = step.undo() instanceof MifosFlow.Definition.Noop<?> ? null : summary(step.undo());
                var fragment = undo != null && isSingleAction(step.action())
                        ? node(step.action(), label(step.action(), null) + " (undo: " + undo + ")")
                        : node(step.action(), null);
                if (fragment == null) {
                    continue;
                }
                if (last == null) {
                    first = fragment;
                } else {
                    connect(last, fragment.in());
                }
                last = fragment;
            }

            return first == null ? null : new Fragment(first.in(), last.out(), last.outLabel());
        }

        /// An expanded sub-process at the top level; inline below it, because kUML lays out one level only.
        private Fragment group(String name, boolean transactional, Function<Compiler, Fragment> content) {
            if (depth > 0) {
                return content.apply(this);
            }

            var id = p.subProcess(name, true, false, transactional, inner -> {
                new Compiler(inner, depth + 1).wrap(content);
                return Unit.INSTANCE;
            });
            activities.add(id);

            return Fragment.of(id);
        }

        // --- nodes and flows ---

        private Fragment task(String label, TaskType type, boolean sequentialMultiInstance) {
            var id = p.task(label, type, t -> {
                if (sequentialMultiInstance) {
                    t.multiInstance(true, null);
                }
                return Unit.INSTANCE;
            });
            activities.add(id);

            return Fragment.of(id);
        }

        /// `from` → `definition` → `to`, or `from` → `to` if the definition draws nothing.
        private void branch(
                String from, MifosFlow.Definition<?> definition, String label, boolean isDefault, String to) {
            var fragment = node(definition, null);

            if (fragment == null) {
                flow(from, to, label, isDefault);
            } else {
                flow(from, fragment.in(), label, isDefault);
                connect(fragment, to);
            }
        }

        private void connect(Fragment from, String to) {
            flow(from.out(), to, from.outLabel(), false);
        }

        private void flow(String from, String to, String label, boolean isDefault) {
            p.sequenceFlow(from, to, null, label, isDefault);
        }

        /// Boundary events are attached to a single task or sub-process, and only at the top level (kUML lays them
        /// out only there).
        private boolean canAttachBoundaryEvent(Fragment fragment) {
            return depth == 0 && fragment.isSingleNode() && activities.contains(fragment.in());
        }

        // --- helpers ---

        /// Whether `definition` is drawn as exactly one task or sub-process at the top level.
        private static boolean isSingleActivity(MifosFlow.Definition<?> definition) {
            return switch (definition) {
                case MifosFlow.Definition.Act<?> _,
                        MifosFlow.Definition.Step<?, ?, ?> _,
                        MifosFlow.Definition.Check<?, ?> _,
                        MifosFlow.Definition.Subflow<?, ?> _,
                        MifosFlow.Definition.Saga<?> _ -> true;
                // a group, or a single action drawn as a task (a named noop becomes a task too, a tap an event)
                case MifosFlow.Definition.Named<?> named -> !(named.body() instanceof MifosFlow.Definition.Tap<?>);
                case MifosFlow.Definition.Within<?> within ->
                    !within.boundary().isNone() || isSingleActivity(within.body());
                // see forEach(): a single action (but not a tap) becomes one multi-instance task
                case MifosFlow.Definition.ForEach<?, ?> forEach ->
                    isSingleAction(forEach.body()) && !(forEach.body() instanceof MifosFlow.Definition.Tap<?>);
                case MifosFlow.Definition.Sequence<?> sequence -> {
                    var drawn = sequence.steps().stream()
                            .filter(step -> !(step instanceof MifosFlow.Definition.Noop<?>))
                            .toList();
                    yield drawn.size() == 1 && isSingleActivity(drawn.getFirst());
                }
                default -> false;
            };
        }

        private static boolean isSingleAction(MifosFlow.Definition<?> definition) {
            return definition instanceof MifosFlow.Definition.Act<?>
                    || definition instanceof MifosFlow.Definition.Tap<?>
                    || definition instanceof MifosFlow.Definition.Step<?, ?, ?>
                    || definition instanceof MifosFlow.Definition.Check<?, ?>
                    || definition instanceof MifosFlow.Definition.Noop<?>;
        }

        /// The label of a single action: the enclosing name if any, else the usecase name or a generic word.
        private static String label(MifosFlow.Definition<?> definition, String name) {
            if (name != null) {
                return name;
            }
            return switch (definition) {
                case MifosFlow.Definition.Step<?, ?, ?> step -> step.usecase().name();
                case MifosFlow.Definition.Check<?, ?> check -> check.usecase().name();
                case MifosFlow.Definition.Tap<?> _ -> "side effect";
                default -> "act";
            };
        }

        /// A short description of a definition, for labels.
        private static String summary(MifosFlow.Definition<?> definition) {
            return switch (definition) {
                case MifosFlow.Definition.Named<?> named -> named.name();
                case MifosFlow.Definition.Step<?, ?, ?> step -> step.usecase().name();
                case MifosFlow.Definition.Check<?, ?> check -> check.usecase().name();
                case MifosFlow.Definition.Act<?> _ -> "act";
                case MifosFlow.Definition.Tap<?> _ -> "side effect";
                case MifosFlow.Definition.Noop<?> _ -> "nothing";
                case MifosFlow.Definition.Sequence<?> sequence ->
                    sequence.steps().stream()
                            .map(Compiler::summary)
                            .reduce((a, b) -> a + ", then " + b)
                            .orElse("nothing");
                case MifosFlow.Definition.Within<?> within -> summary(within.body());
                default -> definition.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            };
        }
    }
}
