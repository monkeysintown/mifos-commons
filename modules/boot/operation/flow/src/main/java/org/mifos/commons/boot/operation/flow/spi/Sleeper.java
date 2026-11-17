///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.operation.flow.spi;

import java.time.Duration;

/// Pauses between retry attempts. Replaceable so that tests do not have to wait.
@FunctionalInterface
public interface Sleeper {
    /// Sleeps with [Thread#sleep(Duration)].
    Sleeper THREAD = Thread::sleep;

    /// Pauses for the given duration.
    ///
    /// @param duration how long to pause
    /// @throws InterruptedException if the thread is interrupted while pausing
    void sleep(Duration duration) throws InterruptedException;
}
