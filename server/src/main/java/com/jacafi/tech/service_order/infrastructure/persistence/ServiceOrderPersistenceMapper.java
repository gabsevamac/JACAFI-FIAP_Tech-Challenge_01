package com.jacafi.tech.service_order.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.jacafi.tech.service_order.domain.LaunchedService;
import com.jacafi.tech.service_order.domain.ServiceOrder;

@Component
public class ServiceOrderPersistenceMapper {

    private final ServiceJpaRepository serviceJpaRepository;

    public ServiceOrderPersistenceMapper(ServiceJpaRepository serviceJpaRepository) {
        this.serviceJpaRepository = serviceJpaRepository;
    }

    public ServiceOrderJpaEntity toEntity(ServiceOrder order) {
        var entity = new ServiceOrderJpaEntity(
                order.getId(),
                order.getStatus(),
                order.getTotal(),
                order.getVehicleId(),
                order.getCustomerId(),
                order.getRemovedAt().orElse(null),
                null);

        List<LaunchedServiceJpaEntity> launchedEntities = new ArrayList<>();
        for (var launched : order.getLaunchedServices()) {
            var serviceEntity = serviceJpaRepository
                    .findById(launched.getServiceId())
                    .orElseGet(() -> new ServiceJpaEntity(
                            launched.getServiceId(), launched.getServiceDescription(), launched.getPriceAtSale()));
            launchedEntities.add(new LaunchedServiceJpaEntity(
                    entity, serviceEntity, launched.getPriceAtSale(), launched.getQuantity()));
        }
        entity.setLaunchedServices(launchedEntities);
        return entity;
    }

    public void copyInto(ServiceOrderJpaEntity target, ServiceOrder order) {
        target.applyState(
                order.getStatus(), order.getTotal(), order.getRemovedAt().orElse(null));

        // Reconcile launched services
        target.getLaunchedServices().clear();
        for (var launched : order.getLaunchedServices()) {
            var serviceEntity = serviceJpaRepository
                    .findById(launched.getServiceId())
                    .orElseGet(() -> new ServiceJpaEntity(
                            launched.getServiceId(), launched.getServiceDescription(), launched.getPriceAtSale()));
            target.getLaunchedServices()
                    .add(new LaunchedServiceJpaEntity(
                            target, serviceEntity, launched.getPriceAtSale(), launched.getQuantity()));
        }
    }

    public ServiceOrder toDomain(ServiceOrderJpaEntity entity) {
        List<LaunchedService> domainServices = new ArrayList<>();
        for (var item : entity.getLaunchedServices()) {
            domainServices.add(LaunchedService.of(
                    item.getService().getId(),
                    item.getService().getDescription(),
                    item.getPriceAtSale(),
                    item.getQuantity()));
        }

        return ServiceOrder.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .vehicleId(entity.getVehicleId())
                .status(entity.getStatus())
                .total(entity.getTotal())
                .launchedServices(domainServices)
                .registeredAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .removedAt(entity.getDeletedAt().orElse(null))
                .restore();
    }
}
