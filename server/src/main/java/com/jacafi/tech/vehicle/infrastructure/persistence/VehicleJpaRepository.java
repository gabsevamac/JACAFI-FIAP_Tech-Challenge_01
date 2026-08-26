package com.jacafi.tech.vehicle.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository over the storage shape.
 *
 * <p>Every derived query carries {@code AndDeletedAtIsNull}: a removed vehicle keeps its row for
 * the service history and answers nothing. This interface is an implementation detail of the
 * adapters in this package — the application layer talks to the domain port instead.
 *
 * <p>The method names say {@code DeletedAt} while the column says {@code removed_at} and the
 * dictionary says removed (§9, {@code VehicleRemoved}). Spring Data derives a query from the
 * <em>property</em> name, and the property is inherited from {@code AuditableEntity}, which names
 * logical removal once for every slice. The column keeps the vehicle's own vocabulary through an
 * {@code @AttributeOverride} — renaming it would mean rebuilding the partial unique index that
 * guards plate uniqueness, which is a real cost for a cosmetic gain.
 */
interface VehicleJpaRepository extends JpaRepository<VehicleJpaEntity, UUID> {

    Optional<VehicleJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<VehicleJpaEntity> findByLicensePlateAndDeletedAtIsNull(String licensePlate);

    boolean existsByLicensePlateAndDeletedAtIsNull(String licensePlate);

    Page<VehicleJpaEntity> findByCustomerIdAndDeletedAtIsNull(UUID customerId, Pageable pageable);
}
