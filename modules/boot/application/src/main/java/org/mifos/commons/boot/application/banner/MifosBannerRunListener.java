///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.application.banner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;

/// Spring application run listener that installs the Mifos startup banner.
public final class MifosBannerRunListener implements SpringApplicationRunListener {
    /// Creates the listener and registers the Mifos banner with the application.
    ///
    /// @param application the Spring application being started
    /// @param args the command line arguments (unused)
    @SuppressWarnings("java:S1172")
    public MifosBannerRunListener(SpringApplication application, String[] args) {
        application.setBanner(new MifosBanner());
    }
}
