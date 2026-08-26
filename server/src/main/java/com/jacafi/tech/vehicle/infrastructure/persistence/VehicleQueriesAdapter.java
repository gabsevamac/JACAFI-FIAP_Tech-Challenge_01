package com.jacafi.tech.vehicle.infrastructure.persistence;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.infrastructure.persistence.SpringDataPaging;
import com.jacafi.tech.vehicle.application.VehicleQueries;
import com.jacafi.tech.vehicle.domain.Vehicle;

/**
 * Implements the read port. Spring Data's {@code Pageable} stops here: above this class, paging is
 * expressed by the shared {@code PageQuery} and {@code PageResult}.
 */
@Repository
public class VehicleQueriesAdapter implements VehicleQueries {

    /**
     * Where the API's vocabulary and the persistence property names disagree.
     *
     * <p>The response says {@code registeredAt}, because that is the domain event and what §9 of
     * the dictionary fixes. The JPA property says {@code createdAt}, because the audit columns are
     * named identically across every table. Neither should bend to the other, so the mapping is
     * declared here — the one class that already has to know both.
     */
    private static final Map<String, String> PROPERTY_NAMES = Map.of("registeredAt", "createdAt");

    private final VehicleJpaRepository jpaRepository;
    private final VehiclePersistenceMapper mapper;

    public VehicleQueriesAdapter(VehicleJpaRepository jpaRepository, VehiclePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PageResult<Vehicle> findActiveByCustomer(UUID customerId, PageQuery query) {
        Page<VehicleJpaEntity> found = jpaRepository.findByCustomerIdAndDeletedAtIsNull(
                customerId, SpringDataPaging.toPageable(query, PROPERTY_NAMES));

        return SpringDataPaging.toPageResult(found, query, mapper::toDomain);
    }
}
