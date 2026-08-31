package com.jacafi.tech.auth.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.auth.domain.entity.UserAccount;

public interface UserAccountRepositoryPort {
    UserAccount save(UserAccount account);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(UUID id);

    List<UserAccount> findAll();
}
