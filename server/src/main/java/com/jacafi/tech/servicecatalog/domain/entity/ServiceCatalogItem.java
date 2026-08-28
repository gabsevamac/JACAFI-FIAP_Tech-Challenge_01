package com.jacafi.tech.servicecatalog.domain.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** A service offered by the workshop, with the current reference price. */
public final class ServiceCatalogItem {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private final UUID id;
    private final long version;
    private final Instant createdAt;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private boolean active;
    private Instant updatedAt;

    private ServiceCatalogItem(
            UUID id,
            String name,
            String description,
            BigDecimal basePrice,
            boolean active,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.basePrice = requirePrice(basePrice);
        this.active = active;
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ServiceCatalogItem register(
            UUID id, String name, String description, BigDecimal basePrice, Clock clock) {
        Instant now = requireClock(clock).instant();
        return new ServiceCatalogItem(id, name, description, basePrice, true, 0, now, now);
    }

    public static ServiceCatalogItem restore(
            UUID id,
            String name,
            String description,
            BigDecimal basePrice,
            boolean active,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        return new ServiceCatalogItem(id, name, description, basePrice, active, version, createdAt, updatedAt);
    }

    public void update(String name, String description, BigDecimal basePrice, Clock clock) {
        requireActive();
        this.name = requireName(name);
        this.description = normalizeDescription(description);
        this.basePrice = requirePrice(basePrice);
        this.updatedAt = requireClock(clock).instant();
    }

    public void deactivate(Clock clock) {
        requireActive();
        active = false;
        updatedAt = requireClock(clock).instant();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public BigDecimal basePrice() {
        return basePrice;
    }

    public boolean active() {
        return active;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("An inactive catalog item cannot be changed");
        }
    }

    private static Clock requireClock(Clock clock) {
        return Objects.requireNonNull(clock, "clock must not be null");
    }

    private static String requireName(String value) {
        String normalized = normalize(value, "name", MAX_NAME_LENGTH);
        if (normalized == null) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return normalized;
    }

    private static String normalizeDescription(String value) {
        return normalize(value, "description", MAX_DESCRIPTION_LENGTH);
    }

    private static String normalize(String value, String field, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds its maximum length");
        }
        return normalized;
    }

    private static BigDecimal requirePrice(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("basePrice must be a non-negative amount with at most two decimals");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
