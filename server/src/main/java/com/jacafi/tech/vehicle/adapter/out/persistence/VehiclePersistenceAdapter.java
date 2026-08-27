package com.jacafi.tech.vehicle.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.jacafi.tech.shared.adapter.out.persistence.SpringDataPaging;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.VehicleUpdateConflictException;

@Component
public class VehiclePersistenceAdapter implements VehicleRepositoryPort {

    private final VehicleJpaRepository repository;

    public VehiclePersistenceAdapter(VehicleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Vehicle save(Vehicle vehicle, String actor) {
        VehicleJpaEntity entity = repository
                .findById(vehicle.id())
                .map(existing -> update(existing, vehicle, actor))
                .orElseGet(() -> VehiclePersistenceMapper.toJpa(vehicle));
        return VehiclePersistenceMapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public boolean existsActiveByLicensePlate(LicensePlate licensePlate) {
        return repository.existsByLicensePlateAndDeletedAtIsNull(licensePlate.value());
    }

    @Override
    public Optional<Vehicle> findActiveById(UUID vehicleId) {
        return repository.findByIdAndDeletedAtIsNull(vehicleId).map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Vehicle> findActiveByLicensePlate(LicensePlate licensePlate) {
        return repository
                .findByLicensePlateAndDeletedAtIsNull(licensePlate.value())
                .map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Vehicle> findActiveByCustomerId(UUID customerId, PageQuery query) {
        Pageable pageable = SpringDataPaging.toPageable(query, java.util.Map.of("registeredAt", "createdAt"));
        Page<VehicleJpaEntity> page = repository.findByCustomerIdAndDeletedAtIsNull(customerId, pageable);
        return SpringDataPaging.toPageResult(page, query, VehiclePersistenceMapper::toDomain);
    }

    private static VehicleJpaEntity update(VehicleJpaEntity existing, Vehicle vehicle, String actor) {
        if (existing.getVersion() != vehicle.version()) {
            throw new VehicleUpdateConflictException();
        }
        existing.apply(vehicle, actor);
        return existing;
    }
}
