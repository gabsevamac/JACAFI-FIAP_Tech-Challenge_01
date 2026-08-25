package com.jacafi.tech.auth;

/**
 * Credentials submitted on login.
 *
 * <p>Immutable value with no identity, therefore a record. Jackson deserializes record
 * components natively, so no no-args constructor or setters are required.
 *
 * <p>No {@code toString} is declared on purpose: the record default would print the
 * password. Never log an instance of this type.
 */
public record LoginRequestDTO(String username, String password) {

    @Override
    public String toString() {
        // Deliberately opaque: the default record toString would expose the raw password.
        return "LoginRequestDTO[username=***, password=***]";
    }
}
