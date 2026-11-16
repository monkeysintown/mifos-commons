///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.reactive;

import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Flux;

/// Reactive repository for entities identified by a `UUID`.
///
/// @param <T> the entity type
@NoRepositoryBean
public interface MifosPersistenceRelationalReactiveRepository<T> {
    /// Returns all entities.
    ///
    /// @return all entities
    Flux<T> findAll();

    // TODO: finish this!
}
