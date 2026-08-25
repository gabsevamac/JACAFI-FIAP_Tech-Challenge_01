package com.jacafi.tech.features.client.update;

import com.jacafi.tech.features.client.domain.Client;
import com.jacafi.tech.features.client.domain.ClientRepository;
import com.jacafi.tech.features.client.domain.Party;
import com.jacafi.tech.features.client.domain.PersonType;
import com.jacafi.tech.features.client.domain.TaxIdentifier;
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
