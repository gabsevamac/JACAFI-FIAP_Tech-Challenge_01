package com.jacafi.tech.shared.adapter.in.web;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;

import com.jacafi.tech.shared.application.PageQuery;

import io.swagger.v3.oas.annotations.media.Schema;

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

    public static final int MAX_SIZE = 100;

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
