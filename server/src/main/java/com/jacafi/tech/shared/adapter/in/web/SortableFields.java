package com.jacafi.tech.shared.adapter.in.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.jacafi.tech.shared.application.SortCriterion;
import com.jacafi.tech.shared.application.SortDirection;

public final class SortableFields {

    private final Set<String> allowed;
    private final String tieBreaker;

    private SortableFields(Set<String> allowed, String tieBreaker) {
        this.allowed = allowed;
        this.tieBreaker = tieBreaker;
    }

    public static SortableFields of(String tieBreaker, String... allowed) {
        Objects.requireNonNull(tieBreaker, "tieBreaker must not be null");
        Set<String> set = new LinkedHashSet<>(List.of(allowed));
        set.add(tieBreaker);
        return new SortableFields(Set.copyOf(set), tieBreaker);
    }

    public List<SortCriterion> resolve(List<String> requested) {
        List<SortCriterion> criteria = new java.util.ArrayList<>();

        for (String raw : requested) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            String[] parts = raw.split(",", 2);
            String field = parts[0].trim();

            if (!allowed.contains(field)) {

                throw new InvalidPageRequestException("The requested sort field is not supported.");
            }
            criteria.add(new SortCriterion(field, directionOf(parts)));
        }

        if (criteria.stream().noneMatch(c -> c.field().equals(tieBreaker))) {
            criteria.add(SortCriterion.ascending(tieBreaker));
        }
        return List.copyOf(criteria);
    }

    private static SortDirection directionOf(String[] parts) {
        if (parts.length < 2) {
            return SortDirection.ASC;
        }
        String direction = parts[1].trim().toUpperCase(Locale.ROOT);
        return switch (direction) {
            case "ASC" -> SortDirection.ASC;
            case "DESC" -> SortDirection.DESC;
            default -> throw new InvalidPageRequestException("Sort direction must be either asc or desc.");
        };
    }
}
