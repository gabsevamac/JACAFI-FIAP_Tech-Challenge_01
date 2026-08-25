package com.jacafi.tech.features.client.update;

import com.jacafi.tech.features.client.domain.Client;
import com.jacafi.tech.features.client.domain.ClientRepository;
import com.jacafi.tech.features.client.domain.Party;
import com.jacafi.tech.features.client.domain.PersonType;
import com.jacafi.tech.features.client.domain.TaxIdentifier;
import com.jacafi.tech.features.client.query.ClientNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateClientUseCaseTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private UpdateClientUseCase useCase;

    @Test
    void updatesMutableClientData() {
        var id = UUID.randomUUID();
        var client = client();
        when(repository.findById(id)).thenReturn(Optional.of(client));

        var updated = useCase.execute(id, new UpdateClientCommand(
                "Maria da Silva",
                null,
                "novo@example.com",
                "11888888888"));

        assertThat(updated.getParty().getName()).isEqualTo("Maria da Silva");
        assertThat(updated.getEmail()).isEqualTo("novo@example.com");
        assertThat(updated.getParty().getTaxIdentifier().getValue()).isEqualTo("52998224725");
    }

    @Test
    void reportsAMissingClient() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id,
                new UpdateClientCommand("Maria", null, "maria@example.com", "11999999999")))
                .isInstanceOf(ClientNotFoundException.class);
    }

    private Client client() {
        return Client.create(
                Party.create("Maria", null, TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725")),
                "maria@example.com",
                "11999999999");
    }
}
