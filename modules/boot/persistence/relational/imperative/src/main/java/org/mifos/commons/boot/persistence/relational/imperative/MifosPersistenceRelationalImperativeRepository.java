///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.imperative;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.FluentQuery;

/// Imperative (blocking) repository for entities identified by a [UUID], supporting QueryDSL predicates.
///
/// @param <T> the entity type
@NoRepositoryBean
public interface MifosPersistenceRelationalImperativeRepository<T> {
    /// Returns all entities.
    ///
    /// @return all entities
    List<T> findAll();

    /// Returns all entities matching the predicate.
    ///
    /// @param predicate the QueryDSL predicate
    /// @return the matching entities
    List<T> findAll(Predicate predicate);

    /// Returns all entities matching the predicate, sorted.
    ///
    /// @param predicate the QueryDSL predicate
    /// @param sort the sort order
    /// @return the matching entities
    List<T> findAll(Predicate predicate, Sort sort);

    /// Returns all entities matching the predicate, ordered.
    ///
    /// @param predicate the QueryDSL predicate
    /// @param orderSpecifiers the order specifiers
    /// @return the matching entities
    List<T> findAll(Predicate predicate, OrderSpecifier<?>... orderSpecifiers);

    /// Returns all entities, ordered.
    ///
    /// @param orderSpecifiers the order specifiers
    /// @return all entities
    List<T> findAll(OrderSpecifier<?>... orderSpecifiers);

    /// Returns all entities, sorted.
    ///
    /// @param sort the sort order
    /// @return all entities
    Iterable<T> findAll(Sort sort);

    /// Returns a page of entities.
    ///
    /// @param pageable the paging information
    /// @return the requested page
    Page<T> findAll(Pageable pageable);

    /// Saves an entity.
    ///
    /// @param entity the entity to save
    /// @return the saved entity
    T save(T entity);

    /// Saves all given entities.
    ///
    /// @param entities the entities to save
    /// @return the saved entities
    Iterable<T> saveAll(Iterable<T> entities);

    /// Finds an entity by its identifier.
    ///
    /// @param id the identifier
    /// @return the entity, or empty if none exists
    Optional<T> findById(UUID id);

    /// Checks whether an entity with the given identifier exists.
    ///
    /// @param id the identifier
    /// @return `true` if the entity exists
    boolean existsById(UUID id);

    /// Finds all entities with the given identifiers.
    ///
    /// @param ids the identifiers
    /// @return the found entities
    Iterable<T> findAllById(Iterable<UUID> ids);

    /// Counts all entities.
    ///
    /// @return the number of entities
    long count();

    /// Deletes the entity with the given identifier.
    ///
    /// @param id the identifier
    void deleteById(UUID id);

    /// Deletes an entity.
    ///
    /// @param entity the entity to delete
    void delete(T entity);

    /// Deletes all entities with the given identifiers.
    ///
    /// @param ids the identifiers
    void deleteAllById(Iterable<UUID> ids);

    /// Deletes the given entities.
    ///
    /// @param entities the entities to delete
    void deleteAll(Iterable<T> entities);

    /// Deletes all entities.
    void deleteAll();

    /// Finds the single entity matching the predicate.
    ///
    /// @param predicate the QueryDSL predicate
    /// @return the entity, or empty if none matches
    Optional<T> findOne(Predicate predicate);

    /// Returns a page of entities matching the predicate.
    ///
    /// @param predicate the QueryDSL predicate
    /// @param pageable the paging information
    /// @return the requested page
    Page<T> findAll(Predicate predicate, Pageable pageable);

    /// Counts the entities matching the predicate.
    ///
    /// @param predicate the QueryDSL predicate
    /// @return the number of matching entities
    long count(Predicate predicate);

    /// Checks whether any entity matches the predicate.
    ///
    /// @param predicate the QueryDSL predicate
    /// @return `true` if at least one entity matches
    boolean exists(Predicate predicate);

    /// Runs a fluent query for the entities matching the predicate.
    ///
    /// @param <R> the result type
    /// @param predicate the QueryDSL predicate
    /// @param queryFunction defines and executes the fluent query
    /// @return the query result
    <R extends @Nullable Object> R findBy(
            Predicate predicate, Function<FluentQuery.FetchableFluentQuery<T>, R> queryFunction);
}
