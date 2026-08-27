package com.jacafi.tech.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.auth.application.port.AccessTokenPort;
import com.jacafi.tech.auth.application.port.PasswordHashPort;
import com.jacafi.tech.auth.application.port.UserAccountRepositoryPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.entity.UserAccount;
import com.jacafi.tech.auth.domain.exception.AuthenticationFailedException;
import com.jacafi.tech.shared.domain.ErrorCode;

class AuthenticateUserServiceTest {

    private static final UserAccount ACTIVE_ACCOUNT = UserAccount.restore(
            UUID.fromString("10000000-0000-0000-0000-000000000001"),
            "admin",
            "stored-hash",
            Set.of(Role.ADMIN),
            null,
            true);

    @Test
    void authenticatesThroughPasswordAndTokenPorts() {
        FakePasswordHash passwordHash = new FakePasswordHash(true);
        RecordingAccessToken accessToken = new RecordingAccessToken();
        AuthenticateUserService service =
                new AuthenticateUserService(new SingleAccountRepository(ACTIVE_ACCOUNT), passwordHash, accessToken);

        LoginResult result = service.login("admin", "raw-password");

        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.accessToken()).isEqualTo("issued-token");
        assertThat(passwordHash.rawPassword).isEqualTo("raw-password");
        assertThat(passwordHash.storedHash).isEqualTo("stored-hash");
        assertThat(accessToken.subject).isEqualTo("admin");
    }

    @Test
    void rejectsUnknownWrongPasswordAndInactiveAccountsWithoutEnumerationDetail() {
        AuthenticationFailedException unknown = failureFor(null, true);
        AuthenticationFailedException wrongPassword = failureFor(ACTIVE_ACCOUNT, false);
        UserAccount inactive =
                UserAccount.restore(ACTIVE_ACCOUNT.id(), "admin", "stored-hash", Set.of(Role.ADMIN), null, false);
        AuthenticationFailedException deactivated = failureFor(inactive, true);

        assertThat(unknown.errorCode()).isEqualTo(ErrorCode.AUTHENTICATION_REQUIRED);
        assertThat(wrongPassword.errorCode()).isEqualTo(unknown.errorCode());
        assertThat(deactivated.errorCode()).isEqualTo(unknown.errorCode());
        assertThat(wrongPassword.getMessage()).isEqualTo(unknown.getMessage());
        assertThat(deactivated.getMessage()).isEqualTo(unknown.getMessage());
    }

    private static AuthenticationFailedException failureFor(UserAccount account, boolean passwordMatches) {
        AuthenticateUserService service = new AuthenticateUserService(
                new SingleAccountRepository(account),
                new FakePasswordHash(passwordMatches),
                new RecordingAccessToken());
        return (AuthenticationFailedException) assertThatThrownBy(() -> service.login("admin", "password"))
                .isInstanceOf(AuthenticationFailedException.class)
                .actual();
    }

    private static final class FakePasswordHash implements PasswordHashPort {
        private final boolean matches;
        private String rawPassword;
        private String storedHash;

        private FakePasswordHash(boolean matches) {
            this.matches = matches;
        }

        @Override
        public String hash(String rawPassword) {
            return "unused";
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            this.rawPassword = rawPassword;
            this.storedHash = passwordHash;
            return matches;
        }
    }

    private static final class RecordingAccessToken implements AccessTokenPort {
        private String subject;

        @Override
        public String issue(String subject) {
            this.subject = subject;
            return "issued-token";
        }

        @Override
        public String parseSubject(String token) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class SingleAccountRepository implements UserAccountRepositoryPort {
        private final UserAccount account;

        private SingleAccountRepository(UserAccount account) {
            this.account = account;
        }

        @Override
        public UserAccount save(UserAccount account) {
            return account;
        }

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(account);
        }

        @Override
        public Optional<UserAccount> findById(UUID id) {
            return Optional.ofNullable(account);
        }

        @Override
        public List<UserAccount> findAll() {
            return account == null ? List.of() : List.of(account);
        }
    }
}
