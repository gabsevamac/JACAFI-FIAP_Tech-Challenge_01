package com.jacafi.tech.customer.dto;

import com.jacafi.tech.shared.lgpd.PersonalData;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @PersonalData("LGPD Art. 5 I")
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String tradeName,
        @PersonalData("LGPD Art. 5 I")
        @NotBlank @Email @Size(max = 254) String email,
        @PersonalData("LGPD Art. 5 I")
        @NotBlank @Size(max = 20) String phone) {

    /** Never prints the contact data. */
    @Override
    public String toString() {
        return "UpdateCustomerRequest[name=***, tradeName=%s, email=***, phone=***]".formatted(tradeName);
    }
}
