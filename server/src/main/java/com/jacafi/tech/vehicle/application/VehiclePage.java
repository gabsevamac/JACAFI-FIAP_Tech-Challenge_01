package com.jacafi.tech.vehicle.application;

import java.util.List;
import java.util.Objects;

import com.jacafi.tech.vehicle.domain.Vehicle;

/**
 * One page of vehicles, plus what a caller needs to ask for the next one.
 *
 * <p>Lives in the application layer, not the domain: paging is what a screen needs, not what the
 * aggregate guarantees. Declared here rather than reusing Spring Data's {@code Page} so that the
 * layer stays free of the persistence framework and the read port below can be implemented by
 * anything.
 *
 * @param content       the vehicles on this page, never null
 * @param page          zero-based page number
 * @param size          maximum number of vehicles per page
 * @param totalElements how many vehicles match the query in total
 */
public record VehiclePage(List<Vehicle> content, int page, int size, long totalElements) {

    public VehiclePage {
        Objects.requireNonNull(content, "content must not be null");
        content = List.copyOf(content);
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
    }

    public int totalPages() {
        return (int) Math.ceilDiv(totalElements, size);
    }
}
