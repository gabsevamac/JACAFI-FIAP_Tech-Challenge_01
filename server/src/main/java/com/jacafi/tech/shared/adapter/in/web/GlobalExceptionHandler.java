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

/**
 * Turns every failure into RFC 9457 {@code application/problem+json}, and lets nothing else out.
 *
 * <p>The rule, without exception: the client receives a status, a code from {@link ErrorCode}, a
 * sentence written for a human, and a trace id. It never receives a stack trace, SQL, a constraint
 * or index name, a class or property name, a file path, or the value it submitted. Everything the
 * response withholds is in the log under the same trace id, so support loses nothing.
 *
 * <p>Extends {@code ResponseEntityExceptionHandler} to take over the framework's own handlers —
 * otherwise Spring answers those itself, in its own format, past every rule here.
 *
 * <p>Global rather than per slice. The two advices this replaces each mapped
 * {@code IllegalArgumentException} to 400 <em>and copied its message into the body</em>, which
 * published domain invariant messages such as {@code "vehicleId must not be null"} to whoever
 * asked. That is the class of leak a per-slice advice keeps reintroducing: the rule has to be
 * stated once, in one place, for it to be true everywhere.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------------------------------------------------------------------------------
    // As nossas
    // ---------------------------------------------------------------------------------------

    /**
     * Business failures are rendered from the stable catalogue, never from exception text.
     */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        log.warn(
                "Business rule violated [{}] traceId={} {}",
                e.errorCode().code(),
                TraceIdFilter.currentTraceId(),
                LogSafe.value(e.logContext().orElse("")));
        return problem(e.errorCode());
    }

    // ---------------------------------------------------------------------------------------
    // Persistência
    // ---------------------------------------------------------------------------------------

    /**
     * The one this task exists for.
     *
     * <p>Postgres reports a unique index violation as {@code duplicate key value violates unique
     * constraint "ux_vehicles_license_plate_active"} followed by {@code Detail: Key
     * (license_plate)=(ABC1D23) already exists.} — the index name and the plate, in one string.
     * Hibernate wraps it, Spring rewraps it, and any handler that reaches for {@code getMessage()}
     * publishes both: the schema, and personal data the caller may not have been entitled to
     * confirm.
     *
     * <p>Answered generically for that reason. A duplicate the application can foresee is caught
     * before reaching the database and arrives here as a {@code BusinessException} with its own
     * code; what actually lands here is the concurrent case, where two writers passed the same
     * check.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException e) {
        // The full message, index name included, goes to the log — where it is exactly what an
        // operator needs, and where no client can read it.
        log.warn("Data integrity violation [{}]", ErrorCode.DATA_CONFLICT.code(), e);
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

    /**
     * Comes from Spring Data when a sort or filter names a property that does not exist.
     *
     * <p>Its message lists the properties that <em>do</em> exist on the entity, which maps the
     * schema one request at a time. The paging whitelist rejects unknown fields before they get
     * this far; this handler covers whatever route did not go through it.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handlePropertyReference(PropertyReferenceException e) {
        log.warn("Rejected property reference [{}]", ErrorCode.INVALID_PAGING.code(), e);
        return problem(ErrorCode.INVALID_PAGING);
    }

    // ---------------------------------------------------------------------------------------
    // Validação fora do corpo da requisição
    // ---------------------------------------------------------------------------------------

    /** {@code @Validated} on a parameter, rather than {@code @Valid} on a body. */
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

    /**
     * A programmer error that reached the edge: an invariant, a null check, a bad argument.
     *
     * <p>Answered 400 because it is almost always the client's input that provoked it, but with a
     * generic message and never with {@code getMessage()}. Those messages are written for whoever
     * reads the log — {@code "modelYear must not be null"}, {@code "field must not be blank"} —
     * and describe the internals, not anything the caller can act on.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        logClientError(ErrorCode.INVALID_PARAMETER, e);
        return problem(ErrorCode.INVALID_PARAMETER);
    }

    /** A state transition the aggregate refuses. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException e) {
        logClientError(ErrorCode.DATA_CONFLICT, e);
        return problem(ErrorCode.DATA_CONFLICT);
    }

    // ---------------------------------------------------------------------------------------
    // Rede de segurança
    // ---------------------------------------------------------------------------------------

    /**
     * Everything nobody predicted.
     *
     * <p>Logged at ERROR with the whole stack trace, because an unforeseen 500 is the one thing an
     * operator must be able to reconstruct. Answered with a status, a code and the trace id, and
     * nothing else — the exception's class name alone would disclose the stack in use.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error(
                "Unhandled exception [{}] traceId={}",
                ErrorCode.INTERNAL_ERROR.code(),
                TraceIdFilter.currentTraceId(),
                e);
        return problem(ErrorCode.INTERNAL_ERROR);
    }

    // ---------------------------------------------------------------------------------------
    // Handlers do próprio Spring, sobrescritos para passarem pelas mesmas regras
    // ---------------------------------------------------------------------------------------

    /** {@code @Valid} on a request body. Field names and messages, never the submitted values. */
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

        // FieldError.getRejectedValue() holds exactly what the client sent, and is what a naive
        // handler echoes back. For a CPF field that means publishing the registration in the error
        // body — the value is never read here, and that is the point.
        logClientError(ErrorCode.VALIDATION_FAILED, e);
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    /** Malformed JSON, a wrong type inside the body, an unparseable date. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        // Jackson's message quotes the offending fragment of the payload and names the target
        // class and field. Both are withheld: the fragment can carry submitted personal data, and
        // the class name discloses the internal model.
        logClientError(ErrorCode.MALFORMED_BODY, e);
        return ResponseEntity.status(httpStatus(ErrorCode.MALFORMED_BODY)).body(problem(ErrorCode.MALFORMED_BODY));
    }

    /**
     * A path or query parameter that will not convert — {@code ?customerId=abc}.
     *
     * <p>A plain {@code @ExceptionHandler} rather than an override: this exception is not among
     * the ones {@code ResponseEntityExceptionHandler} declares, so there is nothing to override.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        // The parameter name is useful and safe: the client chose it and already knows it. The
        // required type is not — "UUID" or "VehicleStatus" describes the internal model, and the
        // enum's name would enumerate its constants.
        logClientError(ErrorCode.INVALID_PARAMETER, e);
        ProblemDetail problem = problem(ErrorCode.INVALID_PARAMETER);
        problem.setProperty("parameter", e.getName());
        return problem;
    }

    /**
     * Last gate on the framework's own responses.
     *
     * <p>{@code ResponseEntityExceptionHandler} handles a dozen exceptions this class does not
     * override, and its default bodies carry the framework's wording. Routing them through here
     * means every response leaving the application has a code and a trace id, including the ones
     * nobody thought to override.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        ErrorCode code = codeFor(statusCode);
        if (statusCode.is5xxServerError()) {
            log.error("Framework error [{}] traceId={}", code.code(), TraceIdFilter.currentTraceId(), e);
        } else {
            logClientError(code, e);
        }

        // O status vem do statusCode que o framework decidiu, e nao do catalogo. Os dois podem
        // divergir — uma rota inexistente chega aqui como 404 e cairia num codigo generico de 400
        // — e quando divergem o corpo passa a contradizer o cabecalho HTTP, que e pior do que
        // qualquer um dos dois estar errado sozinho.
        return ResponseEntity.status(statusCode).body(problem(code, code.message(), statusCode));
    }

    // ---------------------------------------------------------------------------------------

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

    /**
     * 4xx logs at WARN and without a stack trace.
     *
     * <p>A client sending bad input is not an incident, and a stack trace per malformed request
     * turns the log into noise that hides the 5xx worth reading. The message is kept because it is
     * what makes the entry useful, and it is safe here: only the log sees it.
     */
    private static void logClientError(ErrorCode code, Exception e) {
        log.warn(
                "Client error [{}] traceId={}: {}",
                code.code(),
                TraceIdFilter.currentTraceId(),
                LogSafe.value(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }

    /** {@code registerVehicle.command.licensePlate} to {@code licensePlate}. */
    private static String lastNodeOf(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
    }
}
