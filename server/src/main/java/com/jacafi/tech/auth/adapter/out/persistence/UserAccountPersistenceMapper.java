package com.jacafi.tech.auth.adapter.out.persistence;

import com.jacafi.tech.auth.domain.entity.UserAccount;

final class UserAccountPersistenceMapper {

    private UserAccountPersistenceMapper() {}

    static UserAccountJpaEntity toJpa(UserAccount account) {
        return new UserAccountJpaEntity(
                account.id(),
                account.username(),
                account.passwordHash(),
                account.customerId().orElse(null),
                account.active(),
                account.roles());
    }

    static UserAccount toDomain(UserAccountJpaEntity entity) {
        return UserAccount.restore(
                entity.id(),
                entity.username(),
                entity.passwordHash(),
                entity.roles(),
                entity.customerId(),
                entity.active());
    }
}
