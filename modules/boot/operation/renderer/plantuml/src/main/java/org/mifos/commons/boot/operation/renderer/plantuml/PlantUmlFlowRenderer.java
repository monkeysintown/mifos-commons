///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.renderer.plantuml;

import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.Locale;
import org.mifos.commons.boot.operation.flow.MifosFlow;
import org.mifos.commons.boot.operation.flow.spi.ExecutorRef;
import org.mifos.commons.boot.operation.flow.spi.Renderer;

/// Renders a [MifosFlow.Definition] as a PlantUML activity diagram (the
/// "new" activity syntax).
///
/// Only the definition is needed, not a bound flow, so diagrams can be generated in a unit test, a build step or an
/// actuator endpoint without any usecase, transaction or executor being available. The mapping:
///
/// | Definition                       | Diagram                                                             |
/// |----------------------------------|---------------------------------------------------------------------|
/// | `step`, `check`, `act`           | action; a usecase step is labelled with the usecase's class name    |
/// | `tap`                            | send-signal shaped action                                           |
/// | `named` around a single action   | that action, labelled with the name                                 |
/// | `named` around anything else     | partition                                                           |
/// | `when`                           | `if` / `else` with a generic `when` condition label                 |
/// | `repeatWhile`, `forEach`         | `while` loop; a `forEach` with an empty body is a single action     |
/// | `forkJoin`                       | `fork` / `fork again` / `end fork`                                  |
/// | `within`                         | coloured partition naming the boundary                              |
/// | `retryOn`                        | `repeat` loop that repeats on a retryable failure                   |
/// | `recover`, `saga`, `subflow`     | coloured partition; saga steps carry a note naming their undo step  |
///
/// Conditions, loop items and anonymous `act` steps are lambdas, so the diagram cannot describe them; wrap them in
/// `named` where the diagram is meant for readers.
public final class PlantUmlFlowRenderer implements Renderer {
    // TODO: make this configurable
    static final String BOUNDARY_COLOR = "#E3F2FD";
    static final String RECOVER_COLOR = "#F3E5F5";
    static final String SAGA_COLOR = "#FFF8E1";
    static final String SUBFLOW_COLOR = "#F1F8E9";

    @Override
    public String render(MifosFlow.Definition<?> definition, String title) {
        var builder = new Builder();
        builder.line("@startuml");

        if (title != null) {
            builder.line("title " + builder.oneLine(title));
        }

        builder.line("start");
        builder.node(definition, null);
        builder.line("stop");
        builder.line("@enduml");

        return builder.build();
    }

    private static class Builder {
        private final StringBuilder out = new StringBuilder();
        private int depth;

        // --- nodes ---

        /// @param name the name of an enclosing [MifosFlow.Definition.Named] to use as label for a single action, or
        /// `null`
        private void node(MifosFlow.Definition<?> definition, String name) {
            switch (definition) {
                case MifosFlow.Definition.Noop<?> _ -> {
                    if (name != null) {
                        action(name);
                    }
                }
                case MifosFlow.Definition.Act<?> _ -> action(name != null ? name : "act");
                case MifosFlow.Definition.Tap<?> _ -> line(":" + text(name != null ? name : "side effect") + ">");
                case MifosFlow.Definition.Step<?, ?, ?> step ->
                    action(name != null ? name : step.usecase().name());
                case MifosFlow.Definition.Check<?, ?> check ->
                    action("<size:10>check</size>\n"
                            + (name != null ? name : check.usecase().name()));
                case MifosFlow.Definition.Named<?> named -> {
                    if (isSingleAction(named.body())) {
                        node(named.body(), named.name());
                    } else {
                        partition(null, named.name(), () -> node(named.body(), null));
                    }
                }
                case MifosFlow.Definition.Sequence<?> sequence ->
                    sequence.steps().forEach(step -> node(step, null));
                case MifosFlow.Definition.When<?> when -> {
                    line("if (" + condition("when", "condition?") + ") then (yes)");
                    indented(() -> node(when.then(), null));
                    if (!(when.otherwise() instanceof MifosFlow.Definition.Noop<?>)) {
                        line("else (no)");
                        indented(() -> node(when.otherwise(), null));
                    }
                    line("endif");
                }
                case MifosFlow.Definition.RepeatWhile<?> repeat -> {
                    String label = condition("while", "condition?");
                    line("while (" + label + "\\n<size:10>at most " + repeat.maxIterations()
                            + " times</size>) is (yes)");
                    indented(() -> node(repeat.body(), null));
                    line("endwhile (no)");
                }
                case MifosFlow.Definition.ForEach<?, ?> forEach -> {
                    String items = "item";
                    if (forEach.body() instanceof MifosFlow.Definition.Noop<?>) {
                        action("fold each " + items);
                    } else {
                        line("while (" + condition("for each " + items, null) + ") is (next)");
                        indented(() -> node(forEach.body(), null));
                        line("endwhile (done)");
                    }
                }
                case MifosFlow.Definition.Subflow<?, ?> subflow ->
                    partition(SUBFLOW_COLOR, "subflow", () -> node(subflow.body(), null));
                case MifosFlow.Definition.ForkJoin<?> forkJoin -> forkJoin(forkJoin);
                case MifosFlow.Definition.Within<?> within -> {
                    if (within.boundary().isNone()) {
                        node(within.body(), null);
                    } else {
                        partition(BOUNDARY_COLOR, "within " + within.boundary(), () -> node(within.body(), null));
                    }
                }
                case MifosFlow.Definition.Retry<?> retry -> {
                    line("repeat");
                    indented(() -> node(retry.body(), null));
                    line("repeat while ("
                            + condition("retryable failure?\\n<size:10>policy " + retry.policy() + "</size>", null)
                            + ") is (retry) not (done)");
                }
                case MifosFlow.Definition.Recover<?> recover ->
                    partition(RECOVER_COLOR, "recover expected failures", () -> node(recover.body(), null));
                case MifosFlow.Definition.Saga<?> saga ->
                    partition(SAGA_COLOR, "saga", () -> {
                        for (MifosFlow.Definition.Compensable<?> step : saga.steps()) {
                            node(step.action(), null);
                            if (!(step.undo() instanceof MifosFlow.Definition.Noop<?>)) {
                                line("note right: undo: " + oneLine(summary(step.undo())));
                            }
                        }
                    });
            }
        }

        private void forkJoin(MifosFlow.Definition.ForkJoin<?> forkJoin) {
            List<? extends MifosFlow.Definition<?>> forks = forkJoin.forks();
            if (forks.isEmpty()) {
                return;
            }
            line("fork");
            for (int i = 0; i < forks.size(); i++) {
                if (i > 0) {
                    line("fork again");
                }
                MifosFlow.Definition<?> fork = forks.get(i);
                indented(() -> node(fork, null));
            }
            String executor = forkJoin.executor().equals(ExecutorRef.DEFAULT) ? "" : " on " + forkJoin.executor();
            line("end fork {merge" + oneLine(executor) + "}");
        }

        private void partition(String color, String name, Runnable body) {
            line("partition " + (color == null ? "" : color + " ") + "\"" + name.replace('"', '\'') + "\" {");
            indented(body);
            line("}");
        }

        private void action(String label) {
            line(":" + text(label) + ";");
        }

        // --- helpers ---

        private static boolean isSingleAction(MifosFlow.Definition<?> definition) {
            return definition instanceof MifosFlow.Definition.Act<?>
                    || definition instanceof MifosFlow.Definition.Tap<?>
                    || definition instanceof MifosFlow.Definition.Step<?, ?, ?>
                    || definition instanceof MifosFlow.Definition.Check<?, ?>
                    || definition instanceof MifosFlow.Definition.Noop<?>;
        }

        /// A short description of a definition, for notes.
        private String summary(MifosFlow.Definition<?> definition) {
            return switch (definition) {
                case MifosFlow.Definition.Named<?> named -> named.name();
                case MifosFlow.Definition.Step<?, ?, ?> step -> step.usecase().name();
                case MifosFlow.Definition.Check<?, ?> check -> check.usecase().name();
                case MifosFlow.Definition.Act<?> _ -> "act";
                case MifosFlow.Definition.Tap<?> _ -> "side effect";
                case MifosFlow.Definition.Noop<?> _ -> "nothing";
                case MifosFlow.Definition.Sequence<?> sequence ->
                    sequence.steps().stream()
                            .map(this::summary)
                            .reduce((a, b) -> a + ", then " + b)
                            .orElse("nothing");
                case MifosFlow.Definition.Within<?> within -> summary(within.body());
                default -> definition.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            };
        }

        /// Text for an action: multi-line text uses PlantUML's `\n`; a trailing `;` or shape character would end the
        /// action early, so it is followed by a zero-width space.
        @SuppressWarnings("MethodCanBeStatic")
        private String text(String label) {
            @Var var s = label.replace("\r", "").replace("\n", "\\n");

            if (s.endsWith(";")
                    || s.endsWith(">")
                    || s.endsWith("|")
                    || s.endsWith("<")
                    || s.endsWith("]")
                    || s.endsWith("}")
                    || s.endsWith("/")
                    || s.endsWith("\\")) {
                s = s + "\u200B";
            }

            return s;
        }

        /// Text inside `if (...)`, `while (...)`: parentheses would end the condition, so they become brackets.
        private String condition(String label, String fallback) {
            String s = label == null ? fallback : label;
            return s.replace("\n", "\\n").replace('(', '[').replace(')', ']');
        }

        private String oneLine(String s) {
            return s.replace("\r", "").replace('\n', ' ');
        }

        private void indented(Runnable body) {
            depth++;
            try {
                body.run();
            } finally {
                depth--;
            }
        }

        private void line(String text) {
            out.repeat("  ", depth).append(text).append('\n');
        }

        private String build() {
            return out.toString();
        }
    }
}
