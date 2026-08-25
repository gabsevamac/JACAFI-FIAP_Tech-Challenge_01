package com.jacafi.tech.auth;

/**
 * Payload for creating an employee account.
 *
 * <p>No {@code toString} exposing the password — see {@link LoginRequestDTO}.
 */
public record EmployeeRequestDTO(String username, String password) {

    @Override
    public String toString() {
        return "EmployeeRequestDTO[username=***, password=***]";
    }
}
