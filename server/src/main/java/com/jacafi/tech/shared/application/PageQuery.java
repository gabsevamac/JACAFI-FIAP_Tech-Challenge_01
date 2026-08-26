package com.jacafi.tech.shared.application;

import java.util.List;
import java.util.Objects;

/**
 * What the caller asked for: which page, how large, in what order.
 *
 * <p>Declared here rather than reusing Spring Data's {@code Pageable} so the application layer
 * stays free of the persistence framework — a read port that takes a {@code Pageable} can only
 * ever be implemented by Spring Data, and the conversion belongs in the adapter that already
 * depends on it.
 *
 * <p>Arrives already validated. The bounds and the whitelist are enforced where the request is
 * parsed, so a {@code PageQuery} that exists is one that is safe to run.
 *
 * @param page zero-based
 * @param size number of elements, never above the ceiling the web layer enforces
 * @param sort ordering rules in precedence order, always ending in a unique tie-breaker
 */
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
            // An unordered page query is not a smaller version of an ordered one: without an
            // order the database is free to return rows in any sequence, and "any sequence" may
            // differ between two calls. Page 2 then overlaps or skips page 1 for no visible
            // reason. The whitelist always appends a unique tie-breaker, so this is unreachable
            // through the web layer and exists to catch a caller building one by hand.
            throw new IllegalArgumentException("sort must not be empty: unordered paging is unstable");
        }
    }
}
