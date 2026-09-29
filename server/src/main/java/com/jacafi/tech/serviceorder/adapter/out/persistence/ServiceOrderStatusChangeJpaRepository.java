package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceOrderStatusChangeJpaRepository extends JpaRepository<ServiceOrderStatusChangeJpaEntity, Long> {
    List<ServiceOrderStatusChangeJpaEntity> findByServiceOrderIdOrderById(UUID serviceOrderId);
}
