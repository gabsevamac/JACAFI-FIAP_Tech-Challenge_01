package com.jacafi.tech.serviceorder.application.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OpenServiceOrderCommand(
        UUID customerId,
        UUID vehicleId,
        String reportedIssue,
        List<RequestedService> services,
        List<RequestedMaterial> materials) {
    public OpenServiceOrderCommand {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        Objects.requireNonNull(services, "services must not be null");
        Objects.requireNonNull(materials, "materials must not be null");
        services = List.copyOf(services);
        materials = List.copyOf(materials);
    }

    public record RequestedService(UUID serviceCatalogItemId, int quantity) {
        public RequestedService {
            Objects.requireNonNull(serviceCatalogItemId, "serviceCatalogItemId must not be null");
            if (quantity < 1) throw new IllegalArgumentException("quantity must be at least one");
        }
    }

    public record RequestedMaterial(UUID inventoryItemId, int quantity) {
        public RequestedMaterial {
            Objects.requireNonNull(inventoryItemId, "inventoryItemId must not be null");
            if (quantity < 1) throw new IllegalArgumentException("quantity must be at least one");
        }
    }
}
