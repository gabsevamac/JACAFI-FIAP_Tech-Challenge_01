package com.jacafi.tech.customer.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCustomerRequest(
        @Schema(description = "CPF or CNPJ; formatted values are accepted", example = "529.982.247-25")
        @NotBlank @Size(max = 18) String taxId,

        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String tradeName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 20) String phone) {

    @Override
    public String toString() {
        return "CreateCustomerRequest[taxId=***, name=***, tradeNamePresent=%s]".formatted(tradeName != null);
    }
}
