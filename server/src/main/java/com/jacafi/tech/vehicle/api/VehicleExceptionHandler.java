package com.jacafi.tech.vehicle.api;

import com.jacafi.tech.vehicle.domain.DuplicateLicensePlateException;
import com.jacafi.tech.vehicle.domain.VehicleNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Maps the slice's failures onto RFC 7807 {@code application/problem+json}.
 *
 * <p>Scoped to this slice rather than global. Four people are building four slices in parallel; a
 * single global advice would be a file all of them have to edit, and whoever registered a handler
 * for a broad exception type would silently change the behaviour of everyone else's endpoints.
 *
 * <p>No response body here carries a license plate — not the one that was rejected, not the one
 * that caused a conflict, and not the one that was searched for. The domain exceptions are built
 * that way, and validation messages are written to name the field rather than echo its value.
 */
@RestControllerAdvice(basePackageClasses = VehicleController.class)
public class VehicleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(VehicleExceptionHandler.class);

    @ExceptionHandler(VehicleNotFoundException.class)
    public ProblemDetail handleNotFound(VehicleNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Vehicle not found", e.getMessage());
    }

    /**
     * A duplicate plate is a conflict, not a bad request: the payload is well formed and the
     * caller could not have known the plate was taken.
     */
    @ExceptionHandler(DuplicateLicensePlateException.class)
    public ProblemDetail handleDuplicate(DuplicateLicensePlateException e) {
        return problem(HttpStatus.CONFLICT, "License plate already registered", e.getMessage());
    }

    /**
     * Covers InvalidLicensePlateException, which extends IllegalArgumentException, along with the
     * range and blank checks the aggregate performs and the query parameter checks in the
     * controller. All of them mean the same thing to a client: the request was not acceptable.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    /** Bean validation on the request body, before any use case runs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        List<String> violations = e.getBindingResult().getFieldErrors().stream()
                // Field name and message only. The rejected value is never echoed: for
                // licensePlate that value is personal data (LGPD Art. 6 VII).
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "One or more fields are invalid.");
        problem.setProperty("violations", violations);
        return problem;
    }

    /**
     * An IllegalStateException from the aggregate means the caller asked for a transition the
     * vehicle's state does not allow. It is logged because, unlike the others, reaching it points
     * at a gap in the layers above rather than at client input.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException e) {
        log.warn("Rejected an operation the vehicle's state does not allow", e);
        return problem(HttpStatus.CONFLICT, "Operation not allowed in the current state", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
