package com.jacafi.tech.features.client.update;

import com.jacafi.tech.features.client.domain.ClientRepository;
import com.jacafi.tech.features.client.query.ClientNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class DeactivateClientUseCase {

    private final ClientRepository repository;

    public DeactivateClientUseCase(ClientRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(UUID id) {
        repository.findById(Objects.requireNonNull(id, "Client id must not be null"))
                .orElseThrow(ClientNotFoundException::new)
                .deactivate();
    }
}
