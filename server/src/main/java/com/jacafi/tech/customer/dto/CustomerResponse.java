package com.jacafi.tech.customer.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.customer.entity.Customer;
import com.jacafi.tech.shared.lgpd.PersonalData;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Customer as the API exposes it.
 *
 * <p>No discriminator field. The old {@code personType} is gone from the response as well as from
 * the request: a normalized CPF has eleven characters and a CNPJ fourteen, so the value already
 * says which it is, and a derived field would be one more thing to document, version and keep in
 * step for no information gained.
 */
public record CustomerResponse(
        UUID id,

        @PersonalData("LGPD Art. 5 I — returned in full to an authenticated caller, never logged")
        @Schema(description = "CPF (11 characters) or CNPJ (14); the length tells them apart", example = "52998224725")
        String taxId,

        @PersonalData("LGPD Art. 5 I") String name,
        String tradeName,
        @PersonalData("LGPD Art. 5 I") String email,
        @PersonalData("LGPD Art. 5 I") String phone,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getTaxId().value(),
                customer.getName(),
                customer.getTradeName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.isActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }

    /** Masked and reduced: this object reaches logs like any other. */
    @Override
    public String toString() {
        return "CustomerResponse[id=%s, active=%s]".formatted(id, active);
    }
}
