package com.jacafi.tech.features.client.query;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.exception.ClientNotFoundException;
import com.jacafi.tech.client.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FindClientUseCase {

    private final ClientRepository repository;

    public FindClientUseCase(ClientRepository repository) {
        this.repository = repository;
    }

    public Client byId(UUID id) {
        return repository.findById(Objects.requireNonNull(id, "Client id must not be null"))
                .orElseThrow(ClientNotFoundException::new);
    }

    public Client byTaxIdentifier(PersonType personType, String rawTaxIdentifier) {
        var taxIdentifier = TaxIdentifier.of(personType, rawTaxIdentifier);
        return repository.findByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                        taxIdentifier.getPersonType(),
                        taxIdentifier.getValue())
                .orElseThrow(ClientNotFoundException::new);
    }
}
