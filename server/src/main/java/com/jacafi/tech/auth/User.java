package com.jacafi.tech.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Account used to authenticate against the administrative APIs.
 *
 * <p>A JPA entity cannot be a record: the specification requires a no-args constructor and
 * non-final fields. Hence a plain class with explicit accessors.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    /**
     * Required by JPA, which instantiates entities reflectively before populating their state.
     * Kept {@code protected} so application code cannot build a User in an invalid state —
     * {@link #create(String, String, String)} is the only supported entry point.
     */
    protected User() {
    }

    /**
     * Creates a new account, validating every argument.
     *
     * @param username     unique login name
     * @param passwordHash already-encoded password; this class never encodes and never stores plaintext
     * @param role         authorization role, without the {@code ROLE_} prefix
     */
    public static User create(String username, String passwordHash, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }
        var user = new User();
        user.username = username;
        user.password = passwordHash;
        user.role = role;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    /**
     * Identity-based equality. Comparing every field breaks for managed entities, whose state
     * changes within a persistence context while identity does not. Two unsaved instances
     * (null id) are only equal to themselves.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    /**
     * Prints the identifier and the role only.
     *
     * <p>The password is a credential and the username identifies a natural person, so neither
     * may reach a log, an error message or a stack trace (LGPD Art. 6 VII, Art. 46). This is the
     * behaviour Lombok's {@code @Data} silently broke: it printed every field.
     */
    @Override
    public String toString() {
        return "User[id=%s, role=%s]".formatted(id, role);
    }
}
