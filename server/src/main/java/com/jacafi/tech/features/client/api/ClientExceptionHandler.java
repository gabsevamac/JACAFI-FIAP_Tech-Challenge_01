package com.jacafi.tech.features.client.api;

import com.jacafi.tech.features.client.create.ClientAlreadyExistsException;
import com.jacafi.tech.features.client.domain.InvalidTaxIdentifierException;
import com.jacafi.tech.features.client.query.ClientNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;

@RestControllerAdvice(assignableTypes = ClientController.class)
public class ClientExceptionHandler {

    @ExceptionHandler(ClientNotFoundException.class)
    ProblemDetail notFound(ClientNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({ClientAlreadyExistsException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(Exception exception) {
        var detail = exception instanceof ClientAlreadyExistsException
                ? exception.getMessage()
                : "A client with the provided data already exists";
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler({InvalidTaxIdentifierException.class, IllegalArgumentException.class})
    ProblemDetail badRequest(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }
}
