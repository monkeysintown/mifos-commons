///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.core.model;

/// A Mifos error code: a numeric value identifying the error together with a message key used to resolve the
/// (localized) error message.
public interface MifosErrorCode {
    /// Returns the numeric value of the error code.
    ///
    /// @return the numeric error code
    int getValue();

    /// Returns the symbolic name of the error code (typically the enum constant name).
    ///
    /// @return the error code name
    String getName();

    /// Returns the message key used to resolve the error message from the message bundles.
    ///
    /// @return the message key
    String getKey();
}
