package com.jacafi.tech.vehicle.infrastructure.persistence;

import com.jacafi.tech.vehicle.domain.VehicleAuditEntry;
import com.jacafi.tech.vehicle.domain.VehicleAuditTrail;
import org.springframework.stereotype.Component;

/**
 * Writes the audit trail to its own table, inside the transaction of the operation being audited.
 *
 * <p>Sharing the transaction is deliberate: a write that succeeded without its trail entry, or an
 * entry for a write that rolled back, would both be worse than a failure.
 */
@Component
public class JpaVehicleAuditTrail implements VehicleAuditTrail {

    private final VehicleAuditJpaRepository repository;

    public JpaVehicleAuditTrail(VehicleAuditJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(VehicleAuditEntry entry) {
        repository.save(new VehicleAuditEntryJpaEntity(entry.vehicleId(), entry.operation(),
                entry.actor(), entry.occurredAt()));
    }
}
