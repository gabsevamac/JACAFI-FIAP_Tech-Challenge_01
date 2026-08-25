package com.jacafi.tech.features.client.query;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.Party;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.exception.ClientNotFoundException;
import com.jacafi.tech.client.repository.ClientRepository;
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
class FindClientUseCaseTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private FindClientUseCase useCase;

    @Test
    void findsAClientById() {
        var id = UUID.randomUUID();
        var client = client();
        when(repository.findById(id)).thenReturn(Optional.of(client));

        assertThat(useCase.byId(id)).isSameAs(client);
    }

    @Test
    void findsAClientByNormalizedTaxIdentifier() {
        var client = client();
        when(repository.findByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                PersonType.INDIVIDUAL,
                "52998224725"))
                .thenReturn(Optional.of(client));

        assertThat(useCase.byTaxIdentifier(PersonType.INDIVIDUAL, "529.982.247-25"))
                .isSameAs(client);
    }

    @Test
    void reportsAMissingClient() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.byId(id))
                .isInstanceOf(ClientNotFoundException.class);
    }

    private Client client() {
        return Client.create(
                Party.create("Maria", null, TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725")),
                "maria@example.com",
                "11999999999");
    }
}
