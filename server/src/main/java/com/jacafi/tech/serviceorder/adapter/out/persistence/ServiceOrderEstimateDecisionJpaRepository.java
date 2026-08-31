package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderEstimateDecisionJpaRepository extends JpaRepository<ServiceOrderEstimateDecisionJpaEntity, Long> {
    List<ServiceOrderEstimateDecisionJpaEntity> findByServiceOrderIdOrderById(UUID serviceOrderId);
}
