package com.jacafi.tech.client.repository;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.PersonType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    boolean existsByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
            PersonType personType,
            String value);

    @Override
    @EntityGraph(attributePaths = "party")
    Optional<Client> findById(UUID id);

    @EntityGraph(attributePaths = "party")
    Optional<Client> findByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
            PersonType personType,
            String value);

    @Override
    @EntityGraph(attributePaths = "party")
    Page<Client> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "party")
    Page<Client> findAllByActive(boolean active, Pageable pageable);
}
