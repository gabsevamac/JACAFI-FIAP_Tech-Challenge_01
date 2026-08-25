package com.jacafi.tech.features.client.query;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ListClientsUseCase {

    private final ClientRepository repository;

    public ListClientsUseCase(ClientRepository repository) {
        this.repository = repository;
    }

    public Page<Client> execute(Boolean active, Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        return active == null
                ? repository.findAll(pageable)
                : repository.findAllByActive(active, pageable);
    }
}
