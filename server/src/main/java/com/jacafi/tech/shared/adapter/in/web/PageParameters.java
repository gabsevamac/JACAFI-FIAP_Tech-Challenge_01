package com.jacafi.tech.shared.adapter.in.web;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;

import com.jacafi.tech.shared.application.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The paging query parameters, as one object so springdoc documents them individually.
 *
 * <p>{@code @ParameterObject} is what makes Swagger show {@code page}, {@code size} and
 * {@code sort} as three query parameters with their own descriptions, instead of one opaque
 * request body.
 *
 * <p>The fields are boxed on purpose. {@code int page} would bind a missing parameter to 0 and an
 * explicit {@code ?page=} to 0 as well, making "not asked for" and "asked for zero"
 * indistinguishable — which matters because the defaults belong to this class, not to the binder.
 */
@ParameterObject
public record PageParameters(
        @Schema(description = "Zero-based page number.", defaultValue = "0", minimum = "0")
        Integer page,

        @Schema(description = "Elements per page.", defaultValue = "20", minimum = "1", maximum = "100")
        Integer size,

        @Schema(
                description = "Ordering, as field or field,asc / field,desc. Repeatable. Only the fields"
                        + " documented for the resource are accepted; the identifier is always"
                        + " appended as the final tie-breaker.")
        List<String> sort) {

    public static final int DEFAULT_SIZE = 20;

    /**
     * The ceiling exists for availability, not tidiness: a page size the caller chooses is a page
     * size an attacker chooses, and {@code ?size=100000000} turns one unauthenticated request into
     * a full table scan plus the memory to hold it.
     */
    public static final int MAX_SIZE = 100;

    /**
     * Validates and converts, or refuses.
     *
     * <p>A size above the ceiling is rejected rather than clamped. Silently returning 100 elements
     * to a caller who asked for 500 means their loop over pages skips four fifths of the data and
     * reports success — a wrong answer is worse than an error, because only the error gets fixed.
     */
    public PageQuery toQuery(SortableFields sortable) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;

        if (resolvedPage < 0) {
            throw new InvalidPageRequestException("Page must not be negative.");
        }
        if (resolvedSize < 1) {
            throw new InvalidPageRequestException("Size must be at least 1.");
        }
        if (resolvedSize > MAX_SIZE) {
            throw new InvalidPageRequestException("Size must not exceed " + MAX_SIZE + ".");
        }

        return new PageQuery(resolvedPage, resolvedSize, sortable.resolve(sort == null ? List.of() : sort));
    }
}
