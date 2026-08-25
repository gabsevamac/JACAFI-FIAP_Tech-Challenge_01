package com.jacafi.tech.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClientRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String tradeName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 20) String phone) {
}
