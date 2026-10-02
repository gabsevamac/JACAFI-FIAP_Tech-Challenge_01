package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderEstimateApprovalJpaRepository extends JpaRepository<ServiceOrderEstimateApprovalJpaEntity, Long> {
    List<ServiceOrderEstimateApprovalJpaEntity> findByServiceOrderIdOrderById(UUID serviceOrderId);
}
