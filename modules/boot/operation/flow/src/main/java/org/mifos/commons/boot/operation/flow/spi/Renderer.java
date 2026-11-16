///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import org.mifos.commons.boot.operation.flow.MifosFlow;

/// Renders a [MifosFlow.Definition] as text, e.g. as a diagram.
public interface Renderer {
    /// Renders `definition`; a top-level [MifosFlow.Definition.Named] supplies the title and is not rendered itself.
    ///
    /// @param definition the definition to render
    /// @return the rendered definition
    default String render(MifosFlow.Definition<?> definition) {
        if (definition instanceof MifosFlow.Definition.Named<?>(String name, MifosFlow.Definition<?> body)) {
            return render(body, name);
        }
        return render(definition, null);
    }

    /// Renders `definition` with the given title (`null` for none).
    ///
    /// @param definition the definition to render
    /// @param title the title, or `null` for none
    /// @return the rendered definition
    String render(MifosFlow.Definition<?> definition, String title);
}
