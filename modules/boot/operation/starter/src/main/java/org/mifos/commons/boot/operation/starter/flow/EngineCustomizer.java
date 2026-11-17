///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.starter.flow;

import org.mifos.commons.boot.operation.flow.engine.DefaultFlowEngine;

/// Customizes the [MifosFlow.Engine][org.mifos.commons.boot.operation.flow.MifosFlow.Engine] built by
/// [MifosOperationAutoConfiguration], e.g. to register additional boundaries, executors, retry policies or
/// interceptors. Applied in `@Order` order after all defaults.
@FunctionalInterface
public interface EngineCustomizer {
    /// Customizes the engine builder.
    ///
    /// @param builder the builder, already configured with the default resolvers and interceptors
    void customize(DefaultFlowEngine.Builder builder);
}
