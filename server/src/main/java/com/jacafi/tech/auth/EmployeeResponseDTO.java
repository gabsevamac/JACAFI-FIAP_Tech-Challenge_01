package com.jacafi.tech.auth;

/**
 * Employee account as exposed by the API. Carries no credential.
 */
public record EmployeeResponseDTO(Long id, String username, String role) {}
