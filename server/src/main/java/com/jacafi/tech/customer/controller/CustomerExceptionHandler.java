package com.jacafi.tech.customer.controller;

import com.jacafi.tech.customer.exception.CustomerAlreadyExistsException;
import com.jacafi.tech.customer.exception.CustomerNotFoundException;
import com.jacafi.tech.customer.exception.InvalidTaxIdException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;

@RestControllerAdvice(assignableTypes = CustomerController.class)
public class CustomerExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail notFound(CustomerNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({CustomerAlreadyExistsException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(Exception exception) {
        var detail = exception instanceof CustomerAlreadyExistsException
                ? exception.getMessage()
                : "A customer with the provided data already exists";
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
    }

    @ExceptionHandler({InvalidTaxIdException.class, IllegalArgumentException.class})
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
