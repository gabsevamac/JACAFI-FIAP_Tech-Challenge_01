package com.jacafi.tech.service_order.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.jacafi.tech.service_order.domain.Service;

@Component
public class ServicePersistenceMapper {

    public ServiceJpaEntity toEntity(Service service) {
        return new ServiceJpaEntity(service.getId(), service.getDescription(), service.getBasePrice());
    }

    public void copyInto(ServiceJpaEntity target, Service service) {
        target.applyState(service.getDescription(), service.getBasePrice());
    }

    public Service toDomain(ServiceJpaEntity entity) {
        return Service.builder()
                .id(entity.getId())
                .description(entity.getDescription())
                .basePrice(entity.getBasePrice())
                .restore();
    }
}
