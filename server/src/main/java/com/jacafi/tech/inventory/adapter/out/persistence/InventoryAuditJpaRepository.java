package com.jacafi.tech.inventory.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryAuditJpaRepository extends JpaRepository<InventoryAuditEntryJpaEntity, Long> {}
