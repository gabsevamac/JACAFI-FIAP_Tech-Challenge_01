package com.jacafi.tech.client.dto;

import com.jacafi.tech.client.entity.PersonType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotNull PersonType personType,
        @NotBlank @Size(max = 18) String taxIdentifier,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String tradeName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 20) String phone) {
}
