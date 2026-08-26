package com.jacafi.tech.vehicle.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.jacafi.tech.vehicle.application.VehiclePage;
import com.jacafi.tech.vehicle.application.VehicleQueries;
import com.jacafi.tech.vehicle.domain.Vehicle;

/**
 * Implements the read port. Spring Data's {@code Pageable} stops here: above this class, paging is
 * expressed by the slice's own {@link VehiclePage}.
 */
@Repository
public class VehicleQueriesAdapter implements VehicleQueries {

    /** Stable order, so page two does not overlap page one when rows are added in between. */
    // "createdAt" e o nome da propriedade herdada de AuditableEntity, nao o do dicionario. O Sort
    // do Spring Data e resolvido contra a entidade de persistencia, e e a unica razao de o nome
    // padronizado vazar para fora do mapper. A resposta da API continua dizendo registeredAt.
    private static final Sort BY_REGISTRATION = Sort.by(Sort.Direction.ASC, "createdAt", "id");

    private final VehicleJpaRepository jpaRepository;
    private final VehiclePersistenceMapper mapper;

    public VehicleQueriesAdapter(VehicleJpaRepository jpaRepository, VehiclePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public VehiclePage findActiveByCustomer(UUID customerId, int page, int size) {
        Page<VehicleJpaEntity> found = jpaRepository.findByCustomerIdAndDeletedAtIsNull(
                customerId, PageRequest.of(page, size, BY_REGISTRATION));

        List<Vehicle> content =
                found.getContent().stream().map(mapper::toDomain).toList();
        return new VehiclePage(content, page, size, found.getTotalElements());
    }
}
