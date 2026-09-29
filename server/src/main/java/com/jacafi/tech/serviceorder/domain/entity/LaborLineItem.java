package com.jacafi.tech.serviceorder.domain.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.regex.Pattern;

public record LaborLineItem(
        UUID id, UUID laborOperationId, String laborOperationNameSnapshot, BigDecimal unitPriceSnapshot, int quantity) {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public LaborLineItem {
        if (id == null || laborOperationId == null) {
            throw new IllegalArgumentException("line identifiers must not be null");
        }
        laborOperationNameSnapshot = requireName(laborOperationNameSnapshot);
        unitPriceSnapshot = requirePrice(unitPriceSnapshot);
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least one");
        }
    }

    public static LaborLineItem of(
            UUID id,
            UUID laborOperationId,
            String laborOperationNameSnapshot,
            BigDecimal unitPriceSnapshot,
            int quantity) {
        return new LaborLineItem(id, laborOperationId, laborOperationNameSnapshot, unitPriceSnapshot, quantity);
    }

    public BigDecimal totalAmount() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    private static String requireName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("laborOperationNameSnapshot must not be blank");
        }
        String normalized = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new IllegalArgumentException("laborOperationNameSnapshot must contain between 1 and 120 characters");
        }
        return normalized;
    }

    private static BigDecimal requirePrice(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "unitPriceSnapshot must be a non-negative amount with at most two decimals");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
