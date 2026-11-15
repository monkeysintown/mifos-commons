///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import org.mifos.commons.boot.operation.flow.MifosFlow;

public interface Renderer {
    default String render(MifosFlow.Definition<?> definition) {
        if (definition instanceof MifosFlow.Definition.Named<?>(String name, MifosFlow.Definition<?> body)) {
            return render(name, body);
        }
        return render(null, definition);
    }

    /// Renders `definition` with the given title (`null` for none).
    String render(String title, MifosFlow.Definition<?> definition);
}
