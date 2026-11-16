///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.persistence.relational.imperative;

import com.infobip.spring.data.jdbc.QuerydslJdbcRepository;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

/// Base implementation of [MifosPersistenceRelationalImperativeRepository] delegating to a Spring Data JDBC
/// QueryDSL repository.
///
/// @param <T> the entity type
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractMifosPersistenceRelationalImperativeRepository<T>
        implements MifosPersistenceRelationalImperativeRepository<T> {
    /// The QueryDSL repository all operations are delegated to.
    protected final QuerydslJdbcRepository<T, UUID> repository;

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public List<T> findAll(Predicate predicate) {
        return repository.findAll(predicate);
    }

    @Override
    public List<T> findAll(Predicate predicate, Sort sort) {
        return repository.findAll(predicate, sort);
    }

    @Override
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orderSpecifiers) {
        return repository.findAll(predicate, orderSpecifiers);
    }

    @Override
    public List<T> findAll(OrderSpecifier<?>... orderSpecifiers) {
        return repository.findAll(orderSpecifiers);
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        return repository.findAll(sort);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public T save(T entity) {
        return repository.save(entity);
    }

    @Override
    public Iterable<T> saveAll(Iterable<T> entities) {
        return repository.saveAll(entities);
    }

    @Override
    public Optional<T> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public Iterable<T> findAllById(Iterable<UUID> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void delete(T entity) {
        repository.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<UUID> ids) {
        repository.deleteAllById(ids);
    }

    @Override
    public void deleteAll(Iterable<T> entities) {
        repository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public Optional<T> findOne(Predicate predicate) {
        return repository.findOne(predicate);
    }

    @Override
    public Page<T> findAll(Predicate predicate, Pageable pageable) {
        return repository.findAll(predicate, pageable);
    }

    @Override
    public long count(Predicate predicate) {
        return repository.count(predicate);
    }

    @Override
    public boolean exists(Predicate predicate) {
        return repository.exists(predicate);
    }

    @Override
    public <R> R findBy(Predicate predicate, Function<FluentQuery.FetchableFluentQuery<T>, R> queryFunction) {
        return repository.findBy(predicate, queryFunction);
    }
}
