package com.jacafi.tech.shared.application;

import java.util.Objects;

/**
 * One ordering rule: a field, and which way.
 *
 * <p>{@code field} is a name the application layer understands, already checked against the
 * resource's whitelist by the time it gets here. Nothing downstream re-validates it, which is why
 * building one of these from unchecked client input would be the bug.
 */
public record SortCriterion(String field, SortDirection direction) {

    public SortCriterion {
        Objects.requireNonNull(direction, "direction must not be null");
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
    }

    public static SortCriterion ascending(String field) {
        return new SortCriterion(field, SortDirection.ASC);
    }
}
