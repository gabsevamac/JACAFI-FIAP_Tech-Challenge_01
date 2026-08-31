package com.jacafi.tech.customer.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.customer.domain.entity.Customer;

public record CustomerResponse(
        UUID id,
        String taxId,
        String name,
        String tradeName,
        String email,
        String phone,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id(),
                customer.taxId().value(),
                customer.name(),
                customer.tradeName(),
                customer.email(),
                customer.phone(),
                customer.active(),
                customer.createdAt(),
                customer.updatedAt());
    }

    @Override
    public String toString() {
        return "CustomerResponse[id=%s, active=%s]".formatted(id, active);
    }
}
