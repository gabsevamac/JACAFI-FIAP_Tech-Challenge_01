package com.jacafi.tech.vehicle.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.vehicle.domain.DuplicateLicensePlateException;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

/**
 * Implements the domain port over Spring Data.
 *
 * <p>Also the last line of defence for plate uniqueness. The application layer checks first, so
 * the ordinary duplicate is a business rule violation with a name; what reaches here is the
 * concurrent case, where two registrations of the same plate both passed that check and the
 * partial unique index in the database settles it. Translating the constraint violation back into
 * {@link DuplicateLicensePlateException} means the caller sees one failure, not two.
 */
@Repository
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleJpaRepository jpaRepository;
    private final VehiclePersistenceMapper mapper;

    public VehicleRepositoryAdapter(VehicleJpaRepository jpaRepository, VehiclePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * Transactional because the copy-into-managed-row approach requires it.
     *
     * <p>Outside a transaction, {@code findById} runs in one of its own and hands back a detached
     * instance — mutating that writes nothing, silently. The use cases already open a transaction
     * and this one joins it; the annotation is what keeps the method correct for a caller that
     * does not, which is how the repository's own integration test calls it.
     */
    @Override
    @Transactional
    public void save(Vehicle vehicle) {
        try {
            // Load first, then copy state in — rather than merging a freshly built detached
            // instance. The difference matters because of the optimistic lock: a rebuilt instance
            // carries version 0 every time, so the second write of a row would collide with its own
            // first one. Copying into the managed row lets Hibernate compare versions against the
            // database, where a real conflict is another transaction's write.
            //
            // findById and not findByIdAndDeletedAtIsNull: this same method persists the removal,
            // and by then the row no longer answers the active lookup.
            jpaRepository
                    .findById(vehicle.getId())
                    .ifPresentOrElse(
                            managed -> mapper.copyInto(managed, vehicle),
                            () -> jpaRepository.save(mapper.toEntity(vehicle)));
            jpaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateLicensePlateException("A vehicle with this license plate is already registered.");
        }
    }

    @Override
    public Optional<Vehicle> findActiveById(UUID id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Vehicle> findActiveByLicensePlate(LicensePlate licensePlate) {
        return jpaRepository
                .findByLicensePlateAndDeletedAtIsNull(licensePlate.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveWithLicensePlate(LicensePlate licensePlate) {
        return jpaRepository.existsByLicensePlateAndDeletedAtIsNull(licensePlate.value());
    }
}
