package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderLaborLineJpaRepository extends JpaRepository<ServiceOrderLaborLineJpaEntity, UUID> {
    List<ServiceOrderLaborLineJpaEntity> findByServiceOrderIdAndDeletedAtIsNull(UUID serviceOrderId);
}
