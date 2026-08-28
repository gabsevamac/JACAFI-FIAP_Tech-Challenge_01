package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderEstimateJpaRepository extends JpaRepository<ServiceOrderEstimateJpaEntity, UUID> {
    List<ServiceOrderEstimateJpaEntity> findByServiceOrderIdAndDeletedAtIsNull(UUID serviceOrderId);
}
