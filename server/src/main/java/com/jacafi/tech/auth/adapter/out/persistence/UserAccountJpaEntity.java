package com.jacafi.tech.auth.adapter.out.persistence;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "user_accounts")
public class UserAccountJpaEntity extends AuditableJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_roles", joinColumns = @JoinColumn(name = "user_account_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<Role> roles = new HashSet<>();

    protected UserAccountJpaEntity() {}

    UserAccountJpaEntity(
            UUID id, String username, String passwordHash, UUID customerId, boolean active, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.customerId = customerId;
        this.active = active;
        this.roles = new HashSet<>(roles);
    }

    UUID id() {
        return id;
    }

    String username() {
        return username;
    }

    String passwordHash() {
        return passwordHash;
    }

    UUID customerId() {
        return customerId;
    }

    boolean active() {
        return active;
    }

    Set<Role> roles() {
        return Set.copyOf(roles);
    }
}
