///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.cli.core;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;

/// Entry point of a Mifos CLI implementation: runs on application startup and provides the process exit code.
public interface MifosCliRunner extends CommandLineRunner, ExitCodeGenerator {}
