package com.jacafi.tech.auth.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Test
    void rejectsAnAccountWithoutRoles() {
        assertThatThrownBy(() -> account(Set.of(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresCustomerLinkExactlyWhenCustomerRoleIsPresent() {
        assertThatThrownBy(() -> account(Set.of(Role.CUSTOMER), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account(Set.of(Role.TECHNICIAN), CUSTOMER_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsCustomerAndOperationalRolesTogether() {
        UserAccount account = account(Set.of(Role.CUSTOMER, Role.SERVICE_ADVISOR), CUSTOMER_ID);

        assertThat(account.roles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.SERVICE_ADVISOR);
        assertThat(account.customerId()).contains(CUSTOMER_ID);
    }

    @Test
    void inactiveAccountCannotAuthenticate() {
        UserAccount account = account(Set.of(Role.ADMIN), null);

        account.deactivate();

        assertThat(account.canAuthenticate()).isFalse();
    }

    private static UserAccount account(Set<Role> roles, UUID customerId) {
        return UserAccount.restore(ACCOUNT_ID, "account", "password-hash", roles, customerId, true);
    }
}
