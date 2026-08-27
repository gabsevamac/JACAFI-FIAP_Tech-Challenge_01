package com.jacafi.tech.service_order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * A catalog service launched on a specific service order (the {@code ServiceLineItem} of §4 and §9).
 *
 * <p>Freezes the price at the moment of launch so that subsequent changes to the catalog price-base do
 * not alter agreements already negotiated with the customer.
 *
 * <p>Pure domain model: no framework or persistence dependencies.
 */
public class LaunchedService {

    private static final int PRICE_SCALE = 2;

    private final UUID serviceId;
    private final String serviceDescription;
    private final BigDecimal priceAtSale;
    private int quantity;

    private LaunchedService(UUID serviceId, String serviceDescription, BigDecimal priceAtSale, int quantity) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId must not be null");
        this.serviceDescription = requireDescription(serviceDescription);
        this.priceAtSale = requirePrice(priceAtSale);
        this.quantity = requireQuantity(quantity);
    }

    public static LaunchedService of(Service service, int quantity) {
        Objects.requireNonNull(service, "Service must not be null");
        return of(service.getId(), service.getDescription(), service.getBasePrice(), quantity);
    }

    public static LaunchedService of(Service service, BigDecimal priceAtSale, int quantity) {
        Objects.requireNonNull(service, "Service must not be null");
        return of(service.getId(), service.getDescription(), priceAtSale, quantity);
    }

    public static LaunchedService of(UUID serviceId, String description, BigDecimal priceAtSale, int quantity) {
        return new LaunchedService(serviceId, description, priceAtSale, quantity);
    }

    public BigDecimal getSubtotal() {
        return priceAtSale.multiply(BigDecimal.valueOf(quantity)).setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    public void updateQuantity(int newQuantity) {
        this.quantity = requireQuantity(newQuantity);
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public BigDecimal getPriceAtSale() {
        return priceAtSale;
    }

    public int getQuantity() {
        return quantity;
    }

    private static String requireDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("serviceDescription must not be blank");
        }
        return description.trim();
    }

    private static BigDecimal requirePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("priceAtSale must not be null");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("priceAtSale must be zero or positive");
        }
        if (price.scale() > PRICE_SCALE) {
            throw new IllegalArgumentException("priceAtSale must have at most two decimal places");
        }
        return price.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    private static int requireQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LaunchedService that)) return false;
        return Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId);
    }
}
