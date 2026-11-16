///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.autoconfigure;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.mifos.commons.boot.operation.flow.MifosFlow;
import org.mifos.commons.boot.operation.flow.MifosFlow.Interceptor;
import org.mifos.commons.boot.operation.flow.MifosFlow.MifosFlowContext;
import org.mifos.commons.boot.operation.flow.MifosFlow.Node;

/// Records every step, check and named part of a flow as a Micrometer `Observation` named `mifos.flow`, which
/// yields a timer (`mifos.flow` tagged with `mifos.flow.node` and `mifos.flow.kind`) and a tracing span. Retries
/// are recorded as events on the current observation.
final class ObservationFlowInterceptor implements Interceptor {
    private static final String OBSERVATION_NAME = "mifos.flow";
    private static final String OBSERVATION_NODE = OBSERVATION_NAME + ".node";
    private static final String OBSERVATION_KIND = OBSERVATION_NAME + ".kind";
    private static final String OBSERVATION_PATH = OBSERVATION_NAME + ".path";
    private static final String OBSERVATION_RETRY = OBSERVATION_NAME + ".retry";

    private final ObservationRegistry registry;

    public ObservationFlowInterceptor(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <C extends MifosFlowContext> C intercept(Node node, C context, MifosFlow<C> next) {
        return Observation.createNotStarted(OBSERVATION_NAME, registry)
                .contextualName(node.name())
                .lowCardinalityKeyValue(OBSERVATION_NODE, node.name())
                .lowCardinalityKeyValue(OBSERVATION_KIND, node.kind())
                .highCardinalityKeyValue(OBSERVATION_PATH, node.path())
                .observe(() -> next.run(context));
    }

    @Override
    public void onRetry(Node node, int failedAttempt, RuntimeException cause, Duration delay) {
        var current = registry.getCurrentObservation();

        if (current != null) {
            current.event(Observation.Event.of(
                    OBSERVATION_RETRY,
                    "retry %d of %s after %s"
                            .formatted(
                                    failedAttempt, node.path(), cause.getClass().getSimpleName())));
        }
    }
}
