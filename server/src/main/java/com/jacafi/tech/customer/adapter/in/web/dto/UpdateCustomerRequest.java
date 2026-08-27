package com.jacafi.tech.customer.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String tradeName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 20) String phone) {

    @Override
    public String toString() {
        return "UpdateCustomerRequest[name=***, tradeNamePresent=%s, email=***, phone=***]"
                .formatted(tradeName != null);
    }
}
