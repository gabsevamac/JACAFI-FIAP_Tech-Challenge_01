package com.jacafi.tech.shared.application;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One page of results, in the shape the API returns.
 *
 * <p>Named {@code PageResult} and deliberately <em>not</em> {@code PagedModel}. That name already
 * exists in two places in this ecosystem — {@code org.springframework.data.web.PagedModel} and
 * {@code org.springframework.hateoas.PagedModel} — and a third would make every import ambiguous
 * the moment hypermedia is added.
 *
 * <p>Also not Spring Data's {@code Page}: its JSON shape is an accident of its Java fields, it is
 * not a stable contract, and Spring Data itself advises against serializing it. Returning it would
 * make the API's response format change with a framework upgrade.
 *
 * @param content       the elements on this page, never null
 * @param page          zero-based page number
 * @param size          the page size that was asked for, not the number of elements returned
 * @param totalElements how many elements match in total
 * @param totalPages    how many pages of {@code size} that makes
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResult {
        Objects.requireNonNull(content, "content must not be null");
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }

    /**
     * Derives {@code totalPages} rather than taking it, so the two cannot disagree.
     *
     * <p>{@code size} is checked here and not only in the compact constructor because the division
     * below is an argument to that constructor, and arguments are evaluated first. Leaving it to
     * the constructor meant {@code size = 0} surfaced as {@code ArithmeticException: / by zero}
     * from inside {@code Math.ceilDiv} instead of as the validation failure it is — an error whose
     * stack trace points at the JDK rather than at the caller's mistake.
     */
    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        return new PageResult<>(content, page, size, totalElements, (int) Math.ceilDiv(totalElements, size));
    }

    /** Maps the elements and keeps the paging metadata, for the domain-to-response step. */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
