package com.jacafi.tech.customer.application.port;

import java.util.UUID;

public interface CustomerIdentityRepository {

    void link(String subjectId, UUID customerId);
}
