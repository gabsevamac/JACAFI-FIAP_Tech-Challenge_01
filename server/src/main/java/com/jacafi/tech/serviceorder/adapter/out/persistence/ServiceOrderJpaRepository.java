package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ServiceOrderJpaRepository extends JpaRepository<ServiceOrderJpaEntity, UUID> {
    Optional<ServiceOrderJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select serviceOrder from ServiceOrderJpaEntity serviceOrder
            where serviceOrder.deletedAt is null
              and serviceOrder.status in ('IN_PROGRESS', 'AWAITING_APPROVAL', 'UNDER_DIAGNOSIS', 'RECEIVED')
            order by case serviceOrder.status
                when 'IN_PROGRESS' then 1
                when 'AWAITING_APPROVAL' then 2
                when 'UNDER_DIAGNOSIS' then 3
                when 'RECEIVED' then 4
            end, serviceOrder.createdAt asc, serviceOrder.id asc
            """)
    Page<ServiceOrderJpaEntity> findOperationalQueue(Pageable pageable);
}
