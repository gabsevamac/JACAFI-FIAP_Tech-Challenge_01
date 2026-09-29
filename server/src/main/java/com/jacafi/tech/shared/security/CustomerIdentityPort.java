package com.jacafi.tech.shared.security;

import java.util.Optional;
import java.util.UUID;

public interface CustomerIdentityPort {
    Optional<UUID> customerIdBySubject(String subject);
}
