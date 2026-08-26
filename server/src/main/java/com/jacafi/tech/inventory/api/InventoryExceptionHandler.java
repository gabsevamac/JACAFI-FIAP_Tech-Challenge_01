package com.jacafi.tech.inventory.api;

import com.jacafi.tech.inventory.domain.DuplicateMaterialException;
import com.jacafi.tech.inventory.domain.InsufficientStockException;
import com.jacafi.tech.inventory.domain.InventoryItemNotFoundException;
import com.jacafi.tech.inventory.domain.ReservationNotFoundException;
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
 */
@RestControllerAdvice(basePackageClasses = InventoryController.class)
public class InventoryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryExceptionHandler.class);

    @ExceptionHandler(InventoryItemNotFoundException.class)
    public ProblemDetail handleItemNotFound(InventoryItemNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Inventory item not found", e.getMessage());
    }

    /**
     * A missing reservation is 404 and not 409: the caller asked to act on something that is not
     * there. For a withdrawal it also means no approved estimate ever authorized this material,
     * which is a refusal the caller should not be able to work around by retrying.
     */
    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(ReservationNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Reservation not found", e.getMessage());
    }

    /**
     * A duplicate name is a conflict, not a bad request: the payload is well formed and the caller
     * could not have known the name was taken.
     */
    @ExceptionHandler(DuplicateMaterialException.class)
    public ProblemDetail handleDuplicate(DuplicateMaterialException e) {
        return problem(HttpStatus.CONFLICT, "Material already registered", e.getMessage());
    }

    /**
     * Not enough stock is a conflict for the same reason: the request was valid when it was
     * written, and the state of the shelf is what refuses it.
     *
     * <p>Both quantities travel in the body. A caller told only "not enough" has to guess how much
     * to ask for, and neither number is personal data.
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Insufficient stock", e.getMessage());
        problem.setProperty("requested", e.getRequested());
        problem.setProperty("available", e.getAvailable());
        return problem;
    }

    /**
     * Covers the range and blank checks the aggregate performs and the query parameter checks in
     * the controller. All of them mean the same thing to a client: the request was not acceptable.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    /** Bean validation on the request body, before any use case runs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        List<String> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "One or more fields are invalid.");
        problem.setProperty("violations", violations);
        return problem;
    }

    /**
     * An IllegalStateException from the aggregate means the caller asked for something the item's
     * state does not allow — removing an item that still holds reservations, or touching one that
     * was already removed. Logged because, unlike the others, reaching it points at a gap in the
     * layers above rather than at client input.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException e) {
        log.warn("Rejected an operation the inventory item's state does not allow", e);
        return problem(HttpStatus.CONFLICT, "Operation not allowed in the current state", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
