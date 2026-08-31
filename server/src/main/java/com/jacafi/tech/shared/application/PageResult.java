package com.jacafi.tech.shared.application;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

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

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        return new PageResult<>(content, page, size, totalElements, (int) Math.ceilDiv(totalElements, size));
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
