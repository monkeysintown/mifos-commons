///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.starter;

import org.mifos.commons.boot.operation.starter.flow.MifosFlowConfiguration;
import org.mifos.commons.boot.operation.starter.usecase.MifosUsecaseConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(
        afterName = {"org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration"})
@Import({MifosFlowConfiguration.class, MifosUsecaseConfiguration.class})
class MifosOperationAutoConfiguration {}
