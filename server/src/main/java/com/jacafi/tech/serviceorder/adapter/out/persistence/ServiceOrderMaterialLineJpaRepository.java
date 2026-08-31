package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderMaterialLineJpaRepository extends JpaRepository<ServiceOrderMaterialLineJpaEntity, UUID> {
    List<ServiceOrderMaterialLineJpaEntity> findByServiceOrderIdAndDeletedAtIsNull(UUID serviceOrderId);
}
