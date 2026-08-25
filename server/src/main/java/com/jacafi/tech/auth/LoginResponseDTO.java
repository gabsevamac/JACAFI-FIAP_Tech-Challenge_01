package com.jacafi.tech.auth;

/**
 * Result of a successful login.
 *
 * <p>The token is a bearer credential, so {@code toString} masks it.
 */
public record LoginResponseDTO(String username, String token) {

    @Override
    public String toString() {
        return "LoginResponseDTO[username=%s, token=***]".formatted(username);
    }
}
