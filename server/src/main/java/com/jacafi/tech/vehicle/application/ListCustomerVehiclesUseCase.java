package com.jacafi.tech.vehicle.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.domain.Vehicle;

/**
 * Lists the vehicles of one customer, a page at a time.
 *
 * <p>Thin by nature: there is no rule to enforce and nothing to decide, which is why it delegates
 * straight to the read port. It exists so the api layer depends on the application layer for
 * every operation, rather than on a port for this one and on use cases for the others.
 *
 * <p>An unknown customer yields an empty page, not a 404: this slice cannot tell a customer that
 * does not exist from one that owns no vehicle — that knowledge belongs to the customer slice, and
 * reaching into it would cross the boundary that referencing a customer by identifier,
 * rather than by object, exists to keep.
 */
@Service
public class ListCustomerVehiclesUseCase {

    private final VehicleQueries queries;

    public ListCustomerVehiclesUseCase(VehicleQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public PageResult<Vehicle> list(UUID customerId, PageQuery query) {
        return queries.findActiveByCustomer(customerId, query);
    }
}
