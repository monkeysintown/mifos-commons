///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.boot.commons.service;

import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_REQUEST_INSTANCE_EMPTY;
import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_ILLEGAL;
import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_NOT_FOUND;
import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_NULL;
import static org.mifos.commons.boot.core.exception.MifosUsecaseException.MifosUsecaseErrorCode.MIFOS_COMMONS_ERROR_USECASE_RESPONSE_SERIALIZATION;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.mifos.commons.boot.core.exception.MifosUsecaseException;
import org.mifos.commons.boot.core.model.MifosError;
import org.mifos.commons.boot.core.pattern.MifosUsecase;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class MifosUsecaseJsonHelper {
    private final ObjectMapper mapper;

    @SuppressWarnings("unchecked")
    public Class<? extends MifosUsecase.MifosRequest> resolveRequest(@Nullable String type) {
        if (StringUtils.isEmpty(type)) {
            // "Request type not configured. Set 'type' field or '_type' variable."
            throw new MifosUsecaseException(MifosError.of(MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_NULL));
        }

        try {
            var clazz = Class.forName(type);

            if (!MifosUsecase.MifosRequest.class.isAssignableFrom(clazz)) {
                // "Class " + type + " does not implement Request interface"
                throw new MifosUsecaseException(
                        MifosError.of(MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_ILLEGAL, List.of(type)));
            }

            return (Class<? extends MifosUsecase.MifosRequest>) clazz;
        } catch (ClassNotFoundException cnfe) {
            throw new MifosUsecaseException(
                    MifosError.of(MIFOS_COMMONS_ERROR_USECASE_REQUEST_TYPE_NOT_FOUND, cnfe, List.of(type)));
        }
    }

    public MifosUsecase.MifosRequest parseRequest(@Nullable Object input, Class<? extends MifosUsecase.MifosRequest> clazz) {
        if (input == null) {
            // try to create empty instance
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (NoSuchMethodException
                    | InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new MifosUsecaseException(
                        MifosError.of(MIFOS_COMMONS_ERROR_USECASE_REQUEST_INSTANCE_EMPTY, e, List.of(clazz.getName())));
            }
        }

        // if already the correct type, return directly
        if (clazz.isInstance(input)) {
            return clazz.cast(input);
        }

        // parse from JSON string
        var json = input instanceof String s ? s : mapper.writeValueAsString(input);
        return mapper.readValue(json, clazz);
    }

    public String serializeResponse(MifosUsecase.MifosResponse response) {
        try {
            return mapper.writeValueAsString(response);
        } catch (JacksonException je) {
            throw new MifosUsecaseException(MifosError.of(
                    MIFOS_COMMONS_ERROR_USECASE_RESPONSE_SERIALIZATION,
                    je,
                    List.of(response.getClass().getName())));
        }
    }
}
