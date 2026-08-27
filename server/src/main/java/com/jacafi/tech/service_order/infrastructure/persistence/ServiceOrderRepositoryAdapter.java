package com.jacafi.tech.service_order.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.service_order.domain.ServiceOrder;
import com.jacafi.tech.service_order.domain.ServiceOrderRepository;

@Repository
public class ServiceOrderRepositoryAdapter implements ServiceOrderRepository {

    private final ServiceOrderJpaRepository jpaRepository;
    private final ServiceOrderPersistenceMapper mapper;

    public ServiceOrderRepositoryAdapter(
            ServiceOrderJpaRepository jpaRepository, ServiceOrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void save(ServiceOrder serviceOrder) {
        jpaRepository
                .findById(serviceOrder.getId())
                .ifPresentOrElse(
                        managed -> mapper.copyInto(managed, serviceOrder),
                        () -> jpaRepository.save(mapper.toEntity(serviceOrder)));
        jpaRepository.flush();
    }

    @Override
    public Optional<ServiceOrder> findById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    public List<ServiceOrder> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}
