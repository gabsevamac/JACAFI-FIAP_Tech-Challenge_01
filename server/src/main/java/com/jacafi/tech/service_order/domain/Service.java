package com.jacafi.tech.service_order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A service offered by the workshop (e.g. oil change, wheel alignment), independent of any specific
 * service order.
 *
 * <p>Belongs to the supporting subdomain (§3 and §5 of the ubiquitous language dictionary). It
 * defines the catalog and the base price charged for the service.
 *
 * <p>Pure domain model: no framework, no ORM, no HTTP dependencies.
 */
public class Service {

    private static final int MAX_DESCRIPTION_LENGTH = 45;
    private static final int PRICE_SCALE = 2;
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private final UUID id;
    private String description;
    private BigDecimal basePrice;

    private Service(Builder builder) {
        this.id = builder.id;
        this.description = builder.description;
        this.basePrice = builder.basePrice;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Service create(String description, BigDecimal basePrice) {
        return builder().description(description).basePrice(basePrice).register();
    }

    public static Service create(UUID id, String description, BigDecimal basePrice) {
        Objects.requireNonNull(id, "Service id must not be null");
        return builder().id(id).description(description).basePrice(basePrice).register();
    }

    public void update(String description, BigDecimal basePrice) {
        this.description = requireDescription(description);
        this.basePrice = requireBasePrice(basePrice);
    }

    public void updateBasePrice(BigDecimal newPrice) {
        this.basePrice = requireBasePrice(newPrice);
    }

    public void updateDescription(String newDescription) {
        this.description = requireDescription(newDescription);
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    private static String requireDescription(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Service description must not be blank");
        }
        var normalized = WHITESPACE_RUN.matcher(text.trim()).replaceAll(" ");
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "Service description exceeds maximum length of " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return normalized;
    }

    private static BigDecimal requireBasePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Service base price must not be null");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Service base price must be zero or positive");
        }
        if (price.scale() > PRICE_SCALE) {
            throw new IllegalArgumentException("Service base price must have at most two decimal places");
        }
        return price.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Service that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static final class Builder {
        private UUID id;
        private String description;
        private BigDecimal basePrice;

        private Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder basePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
            return this;
        }

        public Service register() {
            if (this.id == null) {
                this.id = UUID.randomUUID();
            }
            this.description = requireDescription(this.description);
            this.basePrice = requireBasePrice(this.basePrice);
            return new Service(this);
        }

        public Service restore() {
            Objects.requireNonNull(this.id, "Service id must not be null when restoring from persistence");
            this.description = requireDescription(this.description);
            this.basePrice = requireBasePrice(this.basePrice);
            return new Service(this);
        }
    }
}
