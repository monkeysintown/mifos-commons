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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.core.exception.MifosUsecaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.pattern.MifosUsecase;
import org.springframework.stereotype.Service;

/// Registry for usecase implementations. Auto-discovers all Usecase beans and indexes them by request type.
@Slf4j
@RequiredArgsConstructor
@Service
public final class MifosUsecaseRegistry {
    private final Map<Class<?>, MifosUsecase<?, ?>> registry = new ConcurrentHashMap<>();

    /// Auto-discovers usecases from Spring context.
    public MifosUsecaseRegistry(List<MifosUsecase<?, ?>> usecases) {
        for (var usecase : usecases) {
            register(usecase);
        }
    }

    /// Register a usecase manually (for testing or dynamic registration).
    public <REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse> void register(MifosUsecase<REQ, RES> usecase) {
        var requestType = usecase.getClass();

        if (registry.containsKey(requestType)) {
            throw new MifosUsecaseException(
                    MifosError.of(MIFOS_COMMONS_ERROR_USECASE_DUPLICATE, List.of(requestType.getName())));
        }

        registry.put(requestType, usecase);
    }

    /// Find usecase for the given request type.
    @SuppressWarnings("unchecked")
    public <REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse> Optional<MifosUsecase<REQ, RES>> find(
            Class<?> requestType) {
        var usecase = registry.get(requestType);

        if (usecase == null) {
            return Optional.empty();
        }

        return Optional.of((MifosUsecase<REQ, RES>) usecase);
    }

    /// Find usecase for a request instance.
    public <REQ extends MifosUsecase.MifosRequest, RES extends MifosUsecase.MifosResponse> Optional<MifosUsecase<REQ, RES>> find(REQ request) {
        return find(request.getClass());
    }

    public MifosUsecase.MifosResponse execute(MifosUsecase.MifosRequest request) {
        return find(request)
                .map(uc -> uc.execute(request))
                .orElseThrow(() -> new MifosUsecaseException(MifosError.of(MIFOS_COMMONS_ERROR_USECASE_NOT_FOUND)));
    }
}
