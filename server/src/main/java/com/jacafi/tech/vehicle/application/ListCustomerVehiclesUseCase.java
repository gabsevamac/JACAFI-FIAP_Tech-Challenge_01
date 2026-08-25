package com.jacafi.tech.vehicle.application;

import com.jacafi.tech.vehicle.domain.CustomerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Lists the vehicles of one customer, a page at a time.
 *
 * <p>Thin by nature: there is no rule to enforce and nothing to decide, which is why it delegates
 * straight to the read port. It exists so the api layer depends on the application layer for
 * every operation, rather than on a port for this one and on use cases for the others.
 *
 * <p>An unknown customer yields an empty page, not a 404: this slice cannot tell a customer that
 * does not exist from one that owns no vehicle — that knowledge belongs to the customer slice, and
 * reaching into it would break the boundary that {@link CustomerId} exists to keep.
 */
@Service
public class ListCustomerVehiclesUseCase {

    private final VehicleQueries queries;

    public ListCustomerVehiclesUseCase(VehicleQueries queries) {
        this.queries = queries;
    }

    @Transactional(readOnly = true)
    public VehiclePage list(UUID customerId, int page, int size) {
        return queries.findActiveByCustomer(new CustomerId(customerId), page, size);
    }
}
