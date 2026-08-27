package com.jacafi.tech.service_order.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceJpaRepository extends JpaRepository<ServiceJpaEntity, UUID> {}
