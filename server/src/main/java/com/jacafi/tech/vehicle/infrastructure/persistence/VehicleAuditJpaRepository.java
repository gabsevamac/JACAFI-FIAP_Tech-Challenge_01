package com.jacafi.tech.vehicle.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only in practice: nothing above it ever calls anything but save. */
interface VehicleAuditJpaRepository extends JpaRepository<VehicleAuditEntryJpaEntity, Long> {
}
