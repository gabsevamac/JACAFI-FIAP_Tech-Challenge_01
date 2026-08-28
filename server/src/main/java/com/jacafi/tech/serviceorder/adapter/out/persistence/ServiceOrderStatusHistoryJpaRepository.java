package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderStatusHistoryJpaRepository extends JpaRepository<ServiceOrderStatusHistoryJpaEntity, Long> {
    List<ServiceOrderStatusHistoryJpaEntity> findByServiceOrderIdOrderById(UUID serviceOrderId);
}
