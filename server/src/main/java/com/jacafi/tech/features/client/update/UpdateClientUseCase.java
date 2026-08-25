package com.jacafi.tech.features.client.update;

import com.jacafi.tech.features.client.domain.Client;
import com.jacafi.tech.features.client.domain.ClientRepository;
import com.jacafi.tech.features.client.query.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class UpdateClientUseCase {

    private final ClientRepository repository;

    public UpdateClientUseCase(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Client execute(UUID id, UpdateClientCommand command) {
        Objects.requireNonNull(command, "Command must not be null");
        var client = repository.findById(Objects.requireNonNull(id, "Client id must not be null"))
                .orElseThrow(ClientNotFoundException::new);

        client.getParty().updateName(command.name(), command.tradeName());
        client.updateContactInformation(command.email(), command.phone());
        return client;
    }
}
