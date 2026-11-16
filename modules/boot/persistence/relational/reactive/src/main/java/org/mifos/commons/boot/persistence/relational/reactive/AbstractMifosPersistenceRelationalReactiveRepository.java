///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.reactive;

import com.infobip.spring.data.r2dbc.QuerydslR2dbcRepository;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

/// Base implementation of [MifosPersistenceRelationalReactiveRepository] delegating to a Spring Data R2DBC
/// QueryDSL repository.
///
/// @param <T> the entity type
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractMifosPersistenceRelationalReactiveRepository<T>
        implements MifosPersistenceRelationalReactiveRepository<T> {
    /// The QueryDSL repository all operations are delegated to.
    protected final QuerydslR2dbcRepository<T, UUID> repository;

    @Override
    public Flux<T> findAll() {
        return repository.findAll();
    }

    // TODO: finish this!
}
