package com.jacafi.tech.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;
import com.jacafi.tech.auth.domain.exception.UsernameAlreadyExistsException;

class UserAccountServiceTest {

    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private final InMemoryAccounts accounts = new InMemoryAccounts();
    private final RecordingPasswordHash passwordHash = new RecordingPasswordHash();
    private AuthenticatedUser principal;
    private UserAccountService service;

    @BeforeEach
    void setUp() {
        UserAccount admin = UserAccount.restore(ADMIN_ID, "admin", "admin-hash", Set.of(Role.ADMIN), null, true);
        accounts.save(admin);
        principal = new AuthenticatedUser(ADMIN_ID, Set.of(Role.ADMIN), null);
        CurrentAuthenticatedUserPort currentUser = () -> principal;
        service = new UserAccountService(accounts, passwordHash, currentUser);
    }

    @Test
    void adminCreatesListsReadsAndDeactivatesAccounts() {
        UserAccount created = service.create("customer", "raw-password", Set.of(Role.CUSTOMER), CUSTOMER_ID);

        assertThat(passwordHash.rawPassword).isEqualTo("raw-password");
        assertThat(created.passwordHash()).isEqualTo("encoded-password");
        assertThat(service.list()).extracting(UserAccount::username).containsExactly("customer");
        assertThat(service.get(created.id())).isSameAs(created);

        service.deactivate(created.id());

        assertThat(accounts.findById(created.id()))
                .get()
                .extracting(UserAccount::active)
                .isEqualTo(false);
    }

    @Test
    void refusesDuplicateUsernameBeforeHashing() {
        assertThatThrownBy(() -> service.create("admin", "raw-password", Set.of(Role.MANAGER), null))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        assertThat(passwordHash.rawPassword).isNull();
    }

    @Test
    void nonAdminCannotListReadCreateOrDeactivateOtherAccounts() {
        principal = new AuthenticatedUser(ADMIN_ID, Set.of(Role.TECHNICIAN), null);

        assertThatThrownBy(service::list).isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(() -> service.get(ADMIN_ID)).isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(() -> service.create("other", "password", Set.of(Role.MANAGER), null))
                .isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(() -> service.deactivate(ADMIN_ID)).isInstanceOf(AccountAccessDeniedException.class);
    }

    @Test
    void currentAccountComesFromAuthenticatedPrincipal() {
        principal = new AuthenticatedUser(ADMIN_ID, Set.of(Role.ADMIN), null);

        assertThat(service.currentAccount().id()).isEqualTo(ADMIN_ID);
    }

    @Test
    void adminCannotManageItsOwnAccountThroughAdministrativeOperations() {
        assertThatThrownBy(() -> service.get(ADMIN_ID)).isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(() -> service.deactivate(ADMIN_ID)).isInstanceOf(AccountAccessDeniedException.class);
    }

    private static final class RecordingPasswordHash implements PasswordHashPort {
        private String rawPassword;

        @Override
        public String hash(String rawPassword) {
            this.rawPassword = rawPassword;
            return "encoded-password";
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryAccounts implements UserAccountRepositoryPort {
        private final Map<UUID, UserAccount> accounts = new LinkedHashMap<>();

        @Override
        public UserAccount save(UserAccount account) {
            accounts.put(account.id(), account);
            return account;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return accounts.values().stream()
                    .filter(account -> account.username().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findById(UUID id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public List<UserAccount> findAll() {
            return new ArrayList<>(accounts.values());
        }
    }
}
