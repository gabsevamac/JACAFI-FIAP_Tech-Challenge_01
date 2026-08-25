package com.jacafi.tech.client.service;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.Party;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.exception.ClientAlreadyExistsException;
import com.jacafi.tech.client.exception.ClientNotFoundException;
import com.jacafi.tech.client.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Client create(
            PersonType personType,
            String rawTaxIdentifier,
            String name,
            String tradeName,
            String email,
            String phone) {
        var taxIdentifier = TaxIdentifier.of(personType, rawTaxIdentifier);

        if (repository.existsByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                taxIdentifier.getPersonType(), taxIdentifier.getValue())) {
            throw new ClientAlreadyExistsException();
        }

        var party = Party.create(name, tradeName, taxIdentifier);
        return repository.save(Client.create(party, email, phone));
    }

    @Transactional(readOnly = true)
    public Client findById(UUID id) {
        return repository.findById(Objects.requireNonNull(id, "Client id must not be null"))
                .orElseThrow(ClientNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Client findByTaxIdentifier(PersonType personType, String rawTaxIdentifier) {
        var taxIdentifier = TaxIdentifier.of(personType, rawTaxIdentifier);
        return repository.findByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                        taxIdentifier.getPersonType(), taxIdentifier.getValue())
                .orElseThrow(ClientNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Page<Client> list(Boolean active, Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        return active == null ? repository.findAll(pageable) : repository.findAllByActive(active, pageable);
    }

    @Transactional
    public Client update(UUID id, String name, String tradeName, String email, String phone) {
        var client = findById(id);
        client.getParty().updateName(name, tradeName);
        client.updateContactInformation(email, phone);
        return client;
    }

    @Transactional
    public void deactivate(UUID id) {
        findById(id).deactivate();
    }
}
