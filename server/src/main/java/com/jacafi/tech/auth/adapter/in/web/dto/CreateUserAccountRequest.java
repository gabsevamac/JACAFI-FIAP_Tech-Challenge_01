package com.jacafi.tech.auth.adapter.in.web.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import com.jacafi.tech.auth.domain.entity.Role;

public record CreateUserAccountRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty Set<Role> roles,
        UUID customerId) {

    @Override
    public String toString() {
        return "CreateUserAccountRequest[username=***, password=***, roles=%s, customerId=%s]"
                .formatted(roles, customerId);
    }
}
