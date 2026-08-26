package com.jacafi.tech.shared.web;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.jacafi.tech.shared.application.SortCriterion;
import com.jacafi.tech.shared.application.SortDirection;

/**
 * The fields one resource allows a client to sort by, and the tie-breaker it always adds.
 *
 * <p>An allow-list rather than a deny-list, for two reasons. Passing the client's string straight
 * to Spring Data lets it resolve any property on the entity, so {@code ?sort=passwordHash} orders
 * by a column the API never exposes — and, more usefully to an attacker, the resulting
 * {@code PropertyReferenceException} names the properties that <em>do</em> exist. A list of
 * accepted names answers every probe identically.
 *
 * <p>Field names here are the API's, and the adapter maps them onto persistence property names.
 * They are not required to match: the response says {@code registeredAt} while the column and the
 * JPA property say {@code createdAt}.
 */
public final class SortableFields {

    private final Set<String> allowed;
    private final String tieBreaker;

    private SortableFields(Set<String> allowed, String tieBreaker) {
        this.allowed = allowed;
        this.tieBreaker = tieBreaker;
    }

    /**
     * @param tieBreaker a field unique per row — in practice the identifier
     * @param allowed    what a client may name, beyond the tie-breaker
     */
    public static SortableFields of(String tieBreaker, String... allowed) {
        Objects.requireNonNull(tieBreaker, "tieBreaker must not be null");
        Set<String> set = new LinkedHashSet<>(List.of(allowed));
        set.add(tieBreaker);
        return new SortableFields(Set.copyOf(set), tieBreaker);
    }

    /**
     * Turns the requested ordering into criteria, appending the tie-breaker.
     *
     * <p>The tie-breaker is what makes paging stable. Ordering by a non-unique column — a
     * registration date, a make — leaves rows that share a value in an order the database may
     * choose differently between two queries, so a row can appear on page 1 and again on page 2,
     * or on neither. Appending a unique field makes the total order deterministic.
     *
     * @throws InvalidPageRequestException when a requested field is not on the list
     */
    public List<SortCriterion> resolve(List<String> requested) {
        List<SortCriterion> criteria = new java.util.ArrayList<>();

        for (String raw : requested) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            // Spring's own convention, kept so clients that already speak it are not surprised:
            // "field" or "field,asc" / "field,desc".
            String[] parts = raw.split(",", 2);
            String field = parts[0].trim();

            if (!allowed.contains(field)) {
                // Says nothing about which field was rejected, nor which ones exist.
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
