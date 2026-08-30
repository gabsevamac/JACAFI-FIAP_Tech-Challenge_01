package com.jacafi.tech.shared.application;

import java.util.Objects;

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
