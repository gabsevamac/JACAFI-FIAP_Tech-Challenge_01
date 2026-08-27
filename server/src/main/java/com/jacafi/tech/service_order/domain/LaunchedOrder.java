package com.jacafi.tech.service_order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a service launched into a service order with frozen price and quantity.
 *
 * <p>Preserves the domain concept of a line item launched against an order ({@code LaunchedOrder} /
 * {@code ServiceLineItem}), independent of persistence mechanics.
 */
public record LaunchedOrder(UUID serviceId, String description, BigDecimal priceAtSale, int quantity) {

    private static final int PRICE_SCALE = 2;

    public LaunchedOrder {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (priceAtSale == null || priceAtSale.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("priceAtSale must be zero or positive");
        }
        if (priceAtSale.scale() > PRICE_SCALE) {
            throw new IllegalArgumentException("priceAtSale must have at most two decimal places");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        priceAtSale = priceAtSale.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    public static LaunchedOrder of(UUID serviceId, String description, BigDecimal priceAtSale, int quantity) {
        return new LaunchedOrder(serviceId, description, priceAtSale, quantity);
    }

    public static LaunchedOrder from(LaunchedService launchedService) {
        Objects.requireNonNull(launchedService, "launchedService must not be null");
        return new LaunchedOrder(
                launchedService.getServiceId(),
                launchedService.getServiceDescription(),
                launchedService.getPriceAtSale(),
                launchedService.getQuantity());
    }

    public BigDecimal getSubtotal() {
        return priceAtSale.multiply(BigDecimal.valueOf(quantity)).setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }
}
