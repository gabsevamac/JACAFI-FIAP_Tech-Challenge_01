package com.jacafi.tech.features.client.update;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.Party;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateClientUseCaseTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private DeactivateClientUseCase useCase;

    @Test
    void deactivatesTheClientWithoutDeletingItsParty() {
        var id = UUID.randomUUID();
        var client = Client.create(
                Party.create("Maria", null, TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725")),
                "maria@example.com",
                "11999999999");
        when(repository.findById(id)).thenReturn(Optional.of(client));

        useCase.execute(id);

        assertThat(client.isActive()).isFalse();
        assertThat(client.getParty()).isNotNull();
    }
}
