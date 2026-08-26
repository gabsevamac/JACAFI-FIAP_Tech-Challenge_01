package com.jacafi.tech.vehicle.application;

import java.util.UUID;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.domain.Vehicle;

/**
 * Read port for queries that serve a screen rather than a business rule.
 *
 * <p>Separate from {@code VehicleRepository}, which loads one aggregate at a time to change it.
 * Reading a page of vehicles changes nothing and enforces nothing, so it does not belong behind
 * the same port — and keeping it here is what stops page and size from appearing in the domain.
 *
 * <p>Restricted to active vehicles, like every other lookup in this slice: a removed vehicle
 * answers no query.
 */
public interface VehicleQueries {

    PageResult<Vehicle> findActiveByCustomer(UUID customerId, PageQuery query);
}
