///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.boot.commons.service;

import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_DUPLICATE;
import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_NOT_FOUND;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.core.exception.MifosUsecaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.pattern.MifosUsecase;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;

/// Registry for usecase implementations. Auto-discovers all Usecase beans and indexes them by request type.
@Slf4j
@RequiredArgsConstructor
@Service
public class MifosUsecaseRegistry {

    private final Map<Class<?>, MifosUsecase<?, ?>> registry = new ConcurrentHashMap<>();

    /// Auto-discovers usecases from Spring context.
    public MifosUsecaseRegistry(List<MifosUsecase<?, ?>> usecases) {
        usecases.forEach(this::register);
    }

    /// Register a usecase manually (for testing or dynamic registration).
    public void register(MifosUsecase<?, ?> usecase) {
        var requestType = resolveRequestType(usecase);

        var existing = registry.putIfAbsent(requestType, usecase);
        if (existing != null) {
            throw new MifosUsecaseException(
                    MifosError.of(MIFOS_COMMONS_ERROR_USECASE_DUPLICATE, List.of(requestType.getName())));
        }

        log.debug(
                "Registered usecase {} for request {}",
                AopProxyUtils.ultimateTargetClass(usecase).getName(),
                requestType.getName());
    }

    /// Find usecase for the given request type.
    @SuppressWarnings("unchecked")
    public <REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse>
            Optional<MifosUsecase<REQ, RES>> find(Class<? extends MifosUsecase.MifosRequest> requestType) {
        return Optional.ofNullable((MifosUsecase<REQ, RES>) registry.get(requestType));
    }

    /// Find usecase for a request instance.
    public <REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse>
            Optional<MifosUsecase<REQ, RES>> find(REQ request) {
        return find(request.getClass());
    }

    public <RES extends MifosUsecase.MifosResponse> RES execute(MifosUsecase.MifosRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        return this.<MifosUsecase.MifosRequest, RES>find(request.getClass())
                .map(uc -> uc.execute(request))
                .orElseThrow(() -> new MifosUsecaseException(MifosError.of(
                        MIFOS_COMMONS_ERROR_USECASE_NOT_FOUND,
                        List.of(request.getClass().getName()))));
    }

    private static Class<?> resolveRequestType(MifosUsecase<?, ?> usecase) {
        var targetClass = AopProxyUtils.ultimateTargetClass(usecase);
        var requestType = ResolvableType.forClass(targetClass)
                .as(MifosUsecase.class)
                .getGeneric(0)
                .resolve();

        if (requestType == null) {
            throw new IllegalStateException("Cannot resolve request type of usecase " + targetClass.getName()
                    + "; implement MifosUsecase with concrete type arguments (no raw types or lambdas)");
        }

        return requestType;
    }
}
