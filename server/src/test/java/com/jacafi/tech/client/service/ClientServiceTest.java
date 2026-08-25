package com.jacafi.tech.client.service;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.Party;
import com.jacafi.tech.client.entity.PersonType;
import com.jacafi.tech.client.entity.TaxIdentifier;
import com.jacafi.tech.client.exception.ClientAlreadyExistsException;
import com.jacafi.tech.client.exception.ClientNotFoundException;
import com.jacafi.tech.client.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ClientService service;

    @Test
    void createsAClientWithANormalizedTaxIdentifier() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var client = service.create(
                PersonType.INDIVIDUAL,
                "529.982.247-25",
                "Maria",
                null,
                "maria@example.com",
                "11999999999");

        assertThat(client.getParty().getTaxIdentifier().getValue()).isEqualTo("52998224725");
        assertThat(client.isActive()).isTrue();
        verify(repository).save(client);
    }

    @Test
    void rejectsAnExistingTaxIdentifier() {
        when(repository.existsByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                PersonType.INDIVIDUAL,
                "52998224725"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                PersonType.INDIVIDUAL,
                "52998224725",
                "Maria",
                null,
                "maria@example.com",
                "11999999999"))
                .isInstanceOf(ClientAlreadyExistsException.class);
    }

    @Test
    void findsAClientById() {
        var id = UUID.randomUUID();
        var client = client();
        when(repository.findById(id)).thenReturn(Optional.of(client));

        assertThat(service.findById(id)).isSameAs(client);
    }

    @Test
    void findsAClientByNormalizedTaxIdentifier() {
        var client = client();
        when(repository.findByParty_TaxIdentifier_PersonTypeAndParty_TaxIdentifier_Value(
                PersonType.INDIVIDUAL,
                "52998224725"))
                .thenReturn(Optional.of(client));

        assertThat(service.findByTaxIdentifier(PersonType.INDIVIDUAL, "529.982.247-25"))
                .isSameAs(client);
    }

    @Test
    void reportsAMissingClient() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void listsAllClientsOrFiltersByActiveState() {
        var page = PageRequest.of(0, 20);
        when(repository.findAll(page)).thenReturn(Page.empty(page));
        when(repository.findAllByActive(true, page)).thenReturn(Page.empty(page));

        assertThat(service.list(null, page)).isEmpty();
        assertThat(service.list(true, page)).isEmpty();
    }

    @Test
    void updatesMutableClientData() {
        var id = UUID.randomUUID();
        var client = client();
        when(repository.findById(id)).thenReturn(Optional.of(client));

        var updated = service.update(id, "Maria da Silva", null, "novo@example.com", "11888888888");

        assertThat(updated.getParty().getName()).isEqualTo("Maria da Silva");
        assertThat(updated.getEmail()).isEqualTo("novo@example.com");
        assertThat(updated.getParty().getTaxIdentifier().getValue()).isEqualTo("52998224725");
    }

    @Test
    void deactivatesTheClientWithoutDeletingItsParty() {
        var id = UUID.randomUUID();
        var client = client();
        when(repository.findById(id)).thenReturn(Optional.of(client));

        service.deactivate(id);

        assertThat(client.isActive()).isFalse();
        assertThat(client.getParty()).isNotNull();
    }

    private Client client() {
        return Client.create(
                Party.create("Maria", null, TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725")),
                "maria@example.com",
                "11999999999");
    }
}
