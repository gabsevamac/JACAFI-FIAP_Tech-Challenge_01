package com.jacafi.tech.auth.application.port;

public interface PasswordHashPort {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
