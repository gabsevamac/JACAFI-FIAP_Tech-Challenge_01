package com.jacafi.tech.service_order.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.service_order.domain.Service;
import com.jacafi.tech.service_order.domain.ServiceRepository;

@Repository
public class ServiceRepositoryAdapter implements ServiceRepository {

    private final ServiceJpaRepository jpaRepository;
    private final ServicePersistenceMapper mapper;

    public ServiceRepositoryAdapter(ServiceJpaRepository jpaRepository, ServicePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void save(Service service) {
        jpaRepository
                .findById(service.getId())
                .ifPresentOrElse(
                        managed -> mapper.copyInto(managed, service),
                        () -> jpaRepository.save(mapper.toEntity(service)));
        jpaRepository.flush();
    }

    @Override
    public Optional<Service> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Service> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
