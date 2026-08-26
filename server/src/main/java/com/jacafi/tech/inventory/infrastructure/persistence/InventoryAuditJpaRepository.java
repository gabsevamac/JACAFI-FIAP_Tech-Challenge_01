package com.jacafi.tech.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only in practice: nothing above it ever calls anything but save. */
interface InventoryAuditJpaRepository extends JpaRepository<InventoryAuditEntryJpaEntity, Long> {
}
