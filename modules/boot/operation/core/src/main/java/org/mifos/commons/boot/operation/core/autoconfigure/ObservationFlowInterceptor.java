///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.core.autoconfigure;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.mifos.commons.boot.operation.core.flow.MifosFlow;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Interceptor;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.MifosFlowContext;
import org.mifos.commons.boot.operation.core.flow.MifosFlow.Node;

/// Records every step, check and named part of a flow as a Micrometer `Observation` named `mifos.flow`, which
/// yields a timer (`mifos.flow` tagged with `mifos.flow.node` and `mifos.flow.kind`) and a tracing span. Retries
/// are recorded as events on the current observation.
public final class ObservationFlowInterceptor implements Interceptor {
    public static final String OBSERVATION_NAME = "mifos.flow";

    private final ObservationRegistry registry;

    public ObservationFlowInterceptor(ObservationRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <C extends MifosFlowContext> C intercept(Node node, C context, MifosFlow<C> next) {
        return Observation.createNotStarted(OBSERVATION_NAME, registry)
                .contextualName(node.name())
                .lowCardinalityKeyValue("mifos.flow.node", node.name())
                .lowCardinalityKeyValue("mifos.flow.kind", node.kind())
                .highCardinalityKeyValue("mifos.flow.path", node.path())
                .observe(() -> next.run(context));
    }

    @Override
    public void onRetry(Node node, int failedAttempt, RuntimeException cause, Duration delay) {
        Observation current = registry.getCurrentObservation();
        if (current != null) {
            current.event(Observation.Event.of(
                    "mifos.flow.retry",
                    "retry " + failedAttempt + " of " + node.path() + " after "
                            + cause.getClass().getSimpleName()));
        }
    }
}
