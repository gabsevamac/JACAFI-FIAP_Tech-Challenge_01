package com.jacafi.tech.auth.adapter.in.web.dto;

import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;

public record UserAccountResponse(UUID id, String username, Set<Role> roles, UUID customerId, boolean active) {

    public static UserAccountResponse from(UserAccount account) {
        return new UserAccountResponse(
                account.id(),
                account.username(),
                account.roles(),
                account.customerId().orElse(null),
                account.active());
    }
}
