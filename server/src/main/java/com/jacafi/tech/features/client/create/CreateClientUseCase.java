package com.jacafi.tech.features.client.create;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.Party;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.exception.ClientAlreadyExistsException;
import com.jacafi.tech.client.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CreateClientUseCase {

    private final ClientRepository repository;

    public CreateClientUseCase(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Client execute(CreateClientCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        var taxIdentifier = TaxIdentifier.of(command.personType(), command.taxIdentifier());

        if (repository.existsByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                taxIdentifier.getPersonType(),
                taxIdentifier.getValue())) {
            throw new ClientAlreadyExistsException();
        }

        var party = Party.create(command.name(), command.tradeName(), taxIdentifier);
        return repository.save(Client.create(party, command.email(), command.phone()));
    }
}
