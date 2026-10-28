///
/// This Source Code Form is subject to the terms of the Mozilla Public
/// License, v. 2.0. If a copy of the MPL was not distributed with this
/// file, You can obtain one at http://mozilla.org/MPL/2.0/.
///
package org.mifos.commons.boot.transport.rest.core;

import static org.mifos.commons.boot.core.MifosCommonsBootConstants.MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE;
import static org.mifos.commons.boot.core.exception.MifosBaseException.getCode;
import static org.mifos.commons.boot.core.exception.MifosException.MifosCommonErrorCode.MIFOS_COMMONS_ERROR_UNKNOWN;
import static org.mifos.commons.boot.core.exception.MifosValidationException.MifosValidationErrorCode.MIFOS_COMMONS_ERROR_VALIDATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.commons.boot.core.MifosCommonsBootProperties;
import org.mifos.commons.boot.core.exception.MifosBaseException;
import org.mifos.commons.boot.core.exception.MifosValidationException;
import org.mifos.commons.boot.core.model.MifosError;
import org.springframework.context.MessageSource;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

/// Global REST controller advice for handling exceptions and producing Problem Detail responses (<a
/// href="https://datatracker.ietf.org/doc/html/rfc7807" target="_blank">RFC 7807</a>) for the Mifos application.
///
/// This advice intercepts exceptions thrown by controller methods and transforms them into structured
/// [org.springframework.http.ProblemDetail] responses. It is designed to handle both low-level framework
/// exceptions and custom Mifos exceptions, providing consistent error formatting with additional "errors" property
/// where
/// applicable.
///
/// The advice is generic over `T extends Throwable` and participates in exception handling for all controller
/// advice methods. It produces responses with content type `application/problem+json`.
///
/// @param <T> the type of throwable that can be handled by the generic handler method; typically [Throwable] or
///     any subtype
/// @see org.springframework.web.bind.annotation.RestControllerAdvice
/// @see org.springframework.http.ProblemDetail
/// @see MifosCommonsBootProperties
/// @see org.springframework.context.MessageSource
/// @author <a href="https://github.com/vidakovic" target="_blank">Aleksandar Vidakovic</a>
/// @since 1.0.0
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class MifosErrorHandlingControllerAdvice<T extends Throwable> {

    private final MessageSource messageSource;
    private final MifosCommonsBootProperties properties;

    @ExceptionHandler(DataBufferLimitException.class)
    public ResponseEntity<ProblemDetail> handleMaxSizeException(DataBufferLimitException e) {
        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(EXPECTATION_FAILED).body(problemDetails);
    }

    // see:
    // https://dev.to/noelopez/spring-rest-exception-handling-problem-details-2hkj#:~:text=ProblemDetail%3A%20The%20main%20class%20representing,additional%2C%20non%2Dstandard%20properties.

    @ExceptionHandler(MifosBaseException.class)
    ResponseEntity<ProblemDetail> handleMifosException(MifosBaseException e, Locale locale) {
        ProblemDetail problemDetails =
                ProblemDetail.forStatusAndDetail(e.getError().getCategory().status(), e.getLocalizedMessage());
        problemDetails.setType(URI.create(properties.getErrorUrl() + "/" + e.getCode()));
        problemDetails.setTitle(e.getCode());
        problemDetails.setDetail(messageSource.getMessage(
                e.getError().getCode().getKey(), e.getError().getArgs().toArray(), locale));
        problemDetails.setProperty(MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE, List.of(e.getError()));

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problemDetails);
    }

    @ExceptionHandler(MifosValidationException.class)
    ResponseEntity<ProblemDetail> handleMifosValidationException(MifosValidationException e) {
        var errors = e.getErrors().getFieldErrors().stream()
                .map(violation -> MifosError.of(
                        MIFOS_COMMONS_ERROR_VALIDATION, violation.getField(), violation.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getLocalizedMessage());
        problemDetails.setType(URI.create(properties.getErrorUrl() + "/validation"));
        problemDetails.setTitle(errors.stream()
                .findFirst()
                .map(mifosError -> getCode(mifosError.getCode().getValue()))
                .orElse(null));
        problemDetails.setProperty(MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE, errors);

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problemDetails);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException e) {
        var errors = e.getConstraintViolations().stream()
                .map(violation -> MifosError.of(
                        MIFOS_COMMONS_ERROR_VALIDATION,
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();

        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getLocalizedMessage());
        problemDetails.setType(URI.create(properties.getErrorUrl() + "/validation"));
        problemDetails.setTitle(errors.stream()
                .findFirst()
                .map(mifosError -> getCode(mifosError.getCode().getValue()))
                .orElse(null));
        problemDetails.setProperty(MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE, errors);

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problemDetails);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ProblemDetail> handleWebExchangeBindException(WebExchangeBindException e) {
        var errors = e.getFieldErrors().stream()
                .map(violation -> MifosError.of(
                        MIFOS_COMMONS_ERROR_VALIDATION, violation.getField(), violation.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getLocalizedMessage());
        problemDetails.setType(URI.create(properties.getErrorUrl() + "/validation"));
        problemDetails.setTitle(errors.stream()
                .findFirst()
                .map(mifosError -> getCode(mifosError.getCode().getValue()))
                .orElse(null));
        problemDetails.setProperty(MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE, errors);

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problemDetails);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        var errors = e.getFieldErrors().stream()
                .map(violation -> MifosError.of(
                        MIFOS_COMMONS_ERROR_VALIDATION, violation.getField(), violation.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getLocalizedMessage());
        problemDetails.setType(URI.create(properties.getErrorUrl() + "/validation"));
        problemDetails.setTitle(errors.stream()
                .findFirst()
                .map(mifosError -> getCode(mifosError.getCode().getValue()))
                .orElse(null));
        problemDetails.setProperty(MIFOS_COMMONS_BOOT_PROBLEM_DETAIL_ERRORS_ATTRIBUTE, errors);

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problemDetails);
    }

    @ExceptionHandler(Throwable.class)
    ResponseEntity<ProblemDetail> handleException(T e, Locale locale) {
        var error = MifosError.of(MIFOS_COMMONS_ERROR_UNKNOWN, e);

        ProblemDetail problemDetails = ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        problemDetails.setType(URI.create(properties.getErrorUrl() + "/error"));
        problemDetails.setTitle(messageSource.getMessage(error.getCode().getKey(), null, locale));

        return ResponseEntity.badRequest().contentType(APPLICATION_PROBLEM_JSON).body(problemDetails);
    }
}
