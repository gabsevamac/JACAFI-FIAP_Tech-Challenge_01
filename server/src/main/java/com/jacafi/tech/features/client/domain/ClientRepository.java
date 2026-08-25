package com.jacafi.tech.features.client.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
            PersonType personType,
            String value);
}
