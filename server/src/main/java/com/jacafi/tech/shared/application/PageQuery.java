package com.jacafi.tech.shared.application;

import java.util.List;
import java.util.Objects;

public record PageQuery(int page, int size, List<SortCriterion> sort) {

    public PageQuery {
        Objects.requireNonNull(sort, "sort must not be null");
        sort = List.copyOf(sort);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (sort.isEmpty()) {

            throw new IllegalArgumentException("sort must not be empty: unordered paging is unstable");
        }
    }
}
