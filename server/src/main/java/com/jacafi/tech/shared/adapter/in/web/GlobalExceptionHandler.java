package com.jacafi.tech.shared.adapter.in.web;

import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.jacafi.tech.shared.domain.BusinessException;
import com.jacafi.tech.shared.domain.ErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        log.warn(
                "Business rule violated [{}] traceId={} {}",
                e.errorCode().code(),
                TraceIdFilter.currentTraceId(),
                LogSafe.value(e.logContext().orElse("")));
        return problem(e.errorCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ignored) {
        log.warn(
                "Data integrity violation [{}] traceId={}",
                ErrorCode.DATA_CONFLICT.code(),
                TraceIdFilter.currentTraceId());
        return problem(ErrorCode.DATA_CONFLICT);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ignored) {
        log.warn(
                "Optimistic locking failure [{}] traceId={}",
                ErrorCode.DATA_CONFLICT.code(),
                TraceIdFilter.currentTraceId());
        return problem(ErrorCode.DATA_CONFLICT);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(PropertyReferenceException ignored) {
        log.warn(
                "Rejected property reference [{}] traceId={}",
                ErrorCode.INVALID_PAGING.code(),
                TraceIdFilter.currentTraceId());
        return problem(ErrorCode.INVALID_PAGING);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        List<Map<String, String>> errors = e.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", lastNodeOf(violation.getPropertyPath().toString()),
                        "message", violation.getMessage()))
                .toList();

        logClientError(ErrorCode.VALIDATION_FAILED, e);
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        logClientError(ErrorCode.INVALID_PARAMETER, e);
        return problem(ErrorCode.INVALID_PARAMETER);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException e) {
        logClientError(ErrorCode.DATA_CONFLICT, e);
        return problem(ErrorCode.DATA_CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error(
                "Unhandled exception [{}] traceId={}",
                ErrorCode.INTERNAL_ERROR.code(),
                TraceIdFilter.currentTraceId(),
                e);
        return problem(ErrorCode.INTERNAL_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field",
                        error.getField(),
                        "message",
                        error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()))
                .toList();

        logClientError(ErrorCode.VALIDATION_FAILED, e);
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        logClientError(ErrorCode.MALFORMED_BODY, e);
        return ResponseEntity.status(httpStatus(ErrorCode.MALFORMED_BODY)).body(problem(ErrorCode.MALFORMED_BODY));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {

        logClientError(ErrorCode.INVALID_PARAMETER, e);
        ProblemDetail problem = problem(ErrorCode.INVALID_PARAMETER);
        problem.setProperty("parameter", e.getName());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        ErrorCode code = codeFor(statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("Framework error [{}] traceId={}", code.code(), TraceIdFilter.currentTraceId(), e);
        } else {
            logClientError(code, e);
        }

        return ResponseEntity.status(statusCode).body(problem(code, code.message(), statusCode));
    }

    private static ErrorCode codeFor(HttpStatusCode status) {
        if (status.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (status.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return ErrorCode.RESOURCE_NOT_FOUND;
        }
        return status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_PARAMETER;
    }

    private static ProblemDetail problem(ErrorCode code) {
        return problem(code, code.message());
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        return problem(code, detail, httpStatus(code));
    }

    private static ProblemDetail problem(ErrorCode code, String detail, HttpStatusCode status) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(HttpStatus.valueOf(status.value()).getReasonPhrase());
        problem.setProperty("code", code.code());
        problem.setProperty("traceId", TraceIdFilter.currentTraceId());
        return problem;
    }

    static HttpStatus httpStatus(ErrorCode code) {
        return switch (code) {
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case MALFORMED_BODY,
                    INVALID_PARAMETER,
                    VALIDATION_FAILED,
                    INVALID_PAGING,
                    INVALID_LICENSE_PLATE,
                    VEHICLE_QUERY_AMBIGUOUS,
                    INVALID_TAX_ID -> HttpStatus.BAD_REQUEST;
            case DATA_CONFLICT,
                    DUPLICATE_LICENSE_PLATE,
                    CUSTOMER_ALREADY_EXISTS,
                    USERNAME_ALREADY_EXISTS,
                    DUPLICATE_MATERIAL,
                    DUPLICATE_SERVICE_CATALOG_ITEM,
                    INSUFFICIENT_STOCK -> HttpStatus.CONFLICT;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case RESOURCE_NOT_FOUND,
                    VEHICLE_NOT_FOUND,
                    CUSTOMER_NOT_FOUND,
                    USER_ACCOUNT_NOT_FOUND,
                    INVENTORY_ITEM_NOT_FOUND,
                    RESERVATION_NOT_FOUND,
                    SERVICE_CATALOG_ITEM_NOT_FOUND,
                    SERVICE_ORDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case AUTHENTICATION_REQUIRED -> HttpStatus.UNAUTHORIZED;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
        };
    }

    private static void logClientError(ErrorCode code, Exception e) {
        log.warn(
                "Client error [{}] traceId={}: {}",
                code.code(),
                TraceIdFilter.currentTraceId(),
                LogSafe.value(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }

    private static String lastNodeOf(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}
