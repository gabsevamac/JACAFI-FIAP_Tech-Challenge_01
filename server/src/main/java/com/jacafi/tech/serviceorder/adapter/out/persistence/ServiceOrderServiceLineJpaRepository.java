package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderServiceLineJpaRepository extends JpaRepository<ServiceOrderServiceLineJpaEntity, UUID> {
    List<ServiceOrderServiceLineJpaEntity> findByServiceOrderIdAndDeletedAtIsNull(UUID serviceOrderId);
}
