package com.jacafi.tech.auth.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.UserAccount;

@Component
public class UserAccountPersistenceAdapter implements UserAccountRepositoryPort {

    private final UserAccountJpaRepository repository;

    public UserAccountPersistenceAdapter(UserAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount account) {
        return UserAccountPersistenceMapper.toDomain(repository.save(UserAccountPersistenceMapper.toJpa(account)));
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return repository.findByUsername(username).map(UserAccountPersistenceMapper::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UUID id) {
        return repository.findById(id).map(UserAccountPersistenceMapper::toDomain);
    }

    @Override
    public List<UserAccount> findAll() {
        return repository.findAll(Sort.by("username")).stream()
                .map(UserAccountPersistenceMapper::toDomain)
                .toList();
    }
}
