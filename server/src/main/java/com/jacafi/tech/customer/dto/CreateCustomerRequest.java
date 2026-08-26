package com.jacafi.tech.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.jacafi.tech.shared.lgpd.PersonalData;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body of a customer registration.
 *
 * <p>No person type: whether the registration is a CPF or a CNPJ follows from the value, and
 * asking the caller to declare it as well only created a way for the two answers to contradict
 * each other.
 */
public record CreateCustomerRequest(
        @PersonalData("LGPD Art. 5 I")
        @Schema(description = "CPF or CNPJ; punctuation is accepted and normalized", example = "529.982.247-25")
        @NotBlank(message = "taxId is required") @Size(max = 18) String taxId,

        @PersonalData("LGPD Art. 5 I") @NotBlank(message = "name is required") @Size(max = 150) String name,

        @Schema(description = "Only for a legal entity") @Size(max = 150) String tradeName,

        @PersonalData("LGPD Art. 5 I") @NotBlank @Email @Size(max = 254) String email,

        @PersonalData("LGPD Art. 5 I") @NotBlank @Size(max = 20) String phone) {

    /** Never prints the registration or the contact data. */
    @Override
    public String toString() {
        return "CreateCustomerRequest[taxId=***, name=***, tradeName=%s]".formatted(tradeName);
    }
}
