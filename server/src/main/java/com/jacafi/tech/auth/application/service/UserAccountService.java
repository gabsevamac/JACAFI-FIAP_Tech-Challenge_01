package com.jacafi.tech.auth.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;
import com.jacafi.tech.auth.domain.exception.UserAccountNotFoundException;
import com.jacafi.tech.auth.domain.exception.UsernameAlreadyExistsException;

public final class UserAccountService {

    private final UserAccountRepositoryPort accounts;
    private final PasswordHashPort passwordHash;
    private final CurrentAuthenticatedUserPort currentUser;

    public UserAccountService(
            UserAccountRepositoryPort accounts,
            PasswordHashPort passwordHash,
            CurrentAuthenticatedUserPort currentUser) {
        this.accounts = accounts;
        this.passwordHash = passwordHash;
        this.currentUser = currentUser;
    }

    public UserAccount create(String username, String rawPassword, Set<Role> roles, UUID customerId) {
        requireAdmin();
        if (accounts.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException();
        }
        UserAccount account =
                UserAccount.create(UUID.randomUUID(), username, passwordHash.hash(rawPassword), roles, customerId);
        return accounts.save(account);
    }

    public List<UserAccount> list() {
        requireAdmin();
        UUID currentUserAccountId = currentUser.currentUser().userAccountId();
        return accounts.findAll().stream()
                .filter(account -> !account.id().equals(currentUserAccountId))
                .toList();
    }

    public UserAccount get(UUID id) {
        requireAdmin();
        requireOtherAccount(id);
        return find(id);
    }

    public void deactivate(UUID id) {
        requireAdmin();
        requireOtherAccount(id);
        UserAccount account = find(id);
        account.deactivate();
        accounts.save(account);
    }

    public UserAccount currentAccount() {
        return find(currentUser.currentUser().userAccountId());
    }

    private UserAccount find(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new UserAccountNotFoundException(id));
    }

    private void requireAdmin() {
        if (!currentUser.currentUser().roles().contains(Role.ADMIN)) {
            throw new AccountAccessDeniedException();
        }
    }

    private void requireOtherAccount(UUID id) {
        if (currentUser.currentUser().userAccountId().equals(id)) {
            throw new AccountAccessDeniedException();
        }
    }
}
