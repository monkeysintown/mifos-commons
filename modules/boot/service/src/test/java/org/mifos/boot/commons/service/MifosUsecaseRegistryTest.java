///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.boot.commons.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class MifosUsecaseRegistryTest {
    private static final String NAME_PARAM = "mifos";
    private final MifosUsecaseRegistry registry = new MifosUsecaseRegistry();

    @BeforeEach
    void setUp() {
        var usecase = new DummyUsecase();

        registry.register(usecase);
    }

    @Test
    void find() {
        var request = new DummyUsecase.Request(NAME_PARAM);

        var usecase = registry.<DummyUsecase.Request, DummyUsecase.Response>find(request)
                .orElse(null);

        assertNotNull(usecase);

        var response = usecase.execute(request);

        assertNotNull(response);
        assertEquals(NAME_PARAM, response.name());
    }

    @Test
    void execute() {
        var request = new DummyUsecase.Request(NAME_PARAM);

        var response = registry.<DummyUsecase.Response>execute(request);

        assertNotNull(response);
        assertEquals(NAME_PARAM, response.name());
    }
}
