///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.flow.util;

import java.util.List;
import org.mifos.commons.boot.operation.core.flow.MifosFlow;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Act;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Check;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Compensable;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.ForEach;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.ForkJoin;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Named;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Noop;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Recover;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.RepeatWhile;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Retry;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Saga;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Sequence;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Step;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Subflow;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Tap;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.When;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Definition.Within;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.ExecutorRef;

/// Renders a [Definition] as a PlantUML activity diagram (the
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
/// | `when`                           | `if` / `else`, labelled with the `when(label, ...)` label           |
/// | `repeatWhile`, `forEach`         | `while` loop                                                        |
/// | `forkJoin`                       | `fork` / `fork again` / `end fork`                                  |
/// | `within`                         | coloured partition naming the boundary                              |
/// | `retryOn`                        | `repeat` loop that repeats on a retryable failure                   |
/// | `recover`, `saga`, `subflow`     | coloured partition; saga steps carry a note naming their undo step  |
///
/// Conditions, loop items and anonymous `act` steps are lambdas, so they can only be described by a label or a
/// name; give them one where the diagram is meant for readers.
@Deprecated(forRemoval = true)
public final class PlantUmlActivityRenderer {
    static final String BOUNDARY_COLOR = "#E3F2FD";
    static final String RECOVER_COLOR = "#F3E5F5";
    static final String SAGA_COLOR = "#FFF8E1";
    static final String SUBFLOW_COLOR = "#F1F8E9";

    private final StringBuilder out = new StringBuilder();
    private int depth;

    private PlantUmlActivityRenderer() {}

    /// Renders `definition`. If it is a [Named] flow, its
    /// name becomes the diagram title.
    public static String render(Definition<?> definition) {
        if (definition instanceof Named<?>(String name, MifosFlow.Definition<?> body)) {
            return render(name, body);
        }
        return render(null, definition);
    }

    /// Renders `definition` with the given title (`null` for none).
    public static String render(String title, Definition<?> definition) {
        var renderer = new PlantUmlActivityRenderer();
        renderer.line("@startuml");
        if (title != null) {
            renderer.line("title " + oneLine(title));
        }
        renderer.line("start");
        renderer.node(definition, null);
        renderer.line("stop");
        renderer.line("@enduml");
        return renderer.out.toString();
    }

    // --- nodes ---

    /// @param name the name of an enclosing [Named] to use as label for a single action, or `null`
    private void node(Definition<?> definition, String name) {
        switch (definition) {
            case Noop<?> noop -> {
                if (name != null) {
                    action(name);
                }
            }
            case Act<?> act -> action(name != null ? name : "act");
            case Tap<?> tap -> line(":" + text(name != null ? name : "side effect") + ">");
            case Step<?, ?, ?> step ->
                action(name != null ? name : step.usecase().name());
            case Check<?, ?> check ->
                action("<size:10>check</size>\n"
                        + (name != null ? name : check.usecase().name()));
            case Named<?> named -> {
                if (isSingleAction(named.body())) {
                    node(named.body(), named.name());
                } else {
                    partition(null, named.name(), () -> node(named.body(), null));
                }
            }
            case Sequence<?> sequence -> sequence.steps().forEach(step -> node(step, null));
            case When<?> when -> {
                line("if (" + condition("when", "condition?") + ") then (yes)");
                indented(() -> node(when.then(), null));
                if (!(when.otherwise() instanceof Noop<?>)) {
                    line("else (no)");
                    indented(() -> node(when.otherwise(), null));
                }
                line("endif");
            }
            case RepeatWhile<?> repeat -> {
                String label = condition("while", "condition?");
                line("while (" + label + "\\n<size:10>at most " + repeat.maxIterations() + " times</size>) is (yes)");
                indented(() -> node(repeat.body(), null));
                line("endwhile (no)");
            }
            case ForEach<?, ?> forEach -> {
                String items = "item";
                if (forEach.body() instanceof Noop<?>) {
                    action("fold each " + items);
                } else {
                    line("while (" + condition("for each " + items, null) + ") is (next)");
                    indented(() -> node(forEach.body(), null));
                    line("endwhile (done)");
                }
            }
            case Subflow<?, ?> subflow -> partition(SUBFLOW_COLOR, "subflow", () -> node(subflow.body(), null));
            case ForkJoin<?> forkJoin -> forkJoin(forkJoin);
            case Within<?> within -> {
                if (within.boundary().isNone()) {
                    node(within.body(), null);
                } else {
                    partition(BOUNDARY_COLOR, "within " + within.boundary(), () -> node(within.body(), null));
                }
            }
            case Retry<?> retry -> {
                line("repeat");
                indented(() -> node(retry.body(), null));
                line("repeat while ("
                        + condition("retryable failure?\\n<size:10>policy " + retry.policy() + "</size>", null)
                        + ") is (retry) not (done)");
            }
            case Recover<?> recover ->
                partition(RECOVER_COLOR, "recover expected failures", () -> node(recover.body(), null));
            case Saga<?> saga ->
                partition(SAGA_COLOR, "saga", () -> {
                    for (Compensable<?> step : saga.steps()) {
                        node(step.action(), null);
                        if (!(step.undo() instanceof Noop<?>)) {
                            line("note right: undo: " + oneLine(summary(step.undo())));
                        }
                    }
                });
        }
    }

    private void forkJoin(ForkJoin<?> forkJoin) {
        List<? extends Definition<?>> forks = forkJoin.forks();
        if (forks.isEmpty()) {
            return;
        }
        line("fork");
        for (int i = 0; i < forks.size(); i++) {
            if (i > 0) {
                line("fork again");
            }
            Definition<?> fork = forks.get(i);
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

    private static boolean isSingleAction(Definition<?> definition) {
        return definition instanceof Act<?>
                || definition instanceof Tap<?>
                || definition instanceof Step<?, ?, ?>
                || definition instanceof Check<?, ?>
                || definition instanceof Noop<?>;
    }

    /// A short description of a definition, for notes.
    static String summary(Definition<?> definition) {
        return switch (definition) {
            case Named<?> named -> named.name();
            case Step<?, ?, ?> step -> step.usecase().name();
            case Check<?, ?> check -> check.usecase().name();
            case Act<?> act -> "act";
            case Tap<?> tap -> "side effect";
            case Noop<?> noop -> "nothing";
            case Sequence<?> sequence ->
                sequence.steps().stream()
                        .map(PlantUmlActivityRenderer::summary)
                        .reduce((a, b) -> a + ", then " + b)
                        .orElse("nothing");
            case Within<?> within -> summary(within.body());
            default -> definition.getClass().getSimpleName().toLowerCase();
        };
    }

    /// Text for an action: multi-line text uses PlantUML's `\n`; a trailing `;` or shape character would end the
    /// action early, so it is followed by a zero-width space.
    private static String text(String label) {
        String s = label.replace("\r", "").replace("\n", "\\n");
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
    private static String condition(String label, String fallback) {
        String s = label == null ? fallback : label;
        return s.replace("\n", "\\n").replace('(', '[').replace(')', ']');
    }

    private static String oneLine(String s) {
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
        out.append("  ".repeat(depth)).append(text).append('\n');
    }
}
