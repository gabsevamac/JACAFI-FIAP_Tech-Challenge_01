package com.jacafi.tech.vehicle.infrastructure.persistence;

import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import org.springframework.stereotype.Component;

/**
 * Moves state between the aggregate and its storage shape.
 *
 * <p>It also owns the anonymization token. The aggregate erases its plate and knows nothing about
 * what takes its place; the column is {@code NOT NULL} and carries a unique index, so something
 * has to go there.
 */
@Component
public class VehiclePersistenceMapper {

    static final String ANONYMIZED_PREFIX = "ANON-";

    public VehicleJpaEntity toEntity(Vehicle vehicle) {
        return new VehicleJpaEntity(vehicle.getId(),
                storedLicensePlate(vehicle),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getModelYear(),
                vehicle.getCustomerId(),
                vehicle.getRegisteredAt(),
                vehicle.getUpdatedAt(),
                vehicle.getRemovedAt().orElse(null));
    }

    public Vehicle toDomain(VehicleJpaEntity entity) {
        return Vehicle.builder()
                .id(entity.getId())
                // A removed row holds a token, not a plate: parsing it would throw, and there is
                // nothing to recover — erasure is the point.
                .licensePlate(entity.getRemovedAt() == null
                        ? new LicensePlate(entity.getLicensePlate())
                        : null)
                .make(entity.getMake())
                .model(entity.getModel())
                .modelYear(entity.getModelYear())
                .customerId(entity.getCustomerId())
                .registeredAt(entity.getRegisteredAt())
                .updatedAt(entity.getUpdatedAt())
                .removedAt(entity.getRemovedAt())
                .restore();
    }

    /**
     * The token derives from the vehicle's own identifier, not from a hash of the plate.
     *
     * <p>Hashing would not be anonymization: Brazilian plates span roughly 26³ × 10³ × 36
     * combinations, which a laptop enumerates in seconds, so any digest of a plate is reversible
     * by brute force. The row identifier carries no information about the plate at all, is unique
     * by construction, and therefore cannot collide with a live registration either.
     */
    private static String storedLicensePlate(Vehicle vehicle) {
        return vehicle.getLicensePlate()
                .map(LicensePlate::value)
                .orElseGet(() -> ANONYMIZED_PREFIX + vehicle.getId());
    }
}
