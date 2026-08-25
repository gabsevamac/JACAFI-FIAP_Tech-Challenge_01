package com.jacafi.tech.features.client.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientTest {

    @Test
    void createsAnActiveClientAndUpdatesContactInformation() {
        var client = Client.create(individual(), " maria@example.com ", " 11999999999 ");

        assertThat(client.isActive()).isTrue();
        assertThat(client.getEmail()).isEqualTo("maria@example.com");
        assertThat(client.getPhone()).isEqualTo("11999999999");

        client.updateContactInformation("novo@example.com", "11888888888");

        assertThat(client.getEmail()).isEqualTo("novo@example.com");
        assertThat(client.getPhone()).isEqualTo("11888888888");
    }

    @Test
    void deactivatesAClientIdempotently() {
        var client = Client.create(individual(), "maria@example.com", "11999999999");

        client.deactivate();
        client.deactivate();

        assertThat(client.isActive()).isFalse();
    }

    @Test
    void rejectsMissingPartyOrContactInformation() {
        assertThatThrownBy(() -> Client.create(null, "maria@example.com", "11999999999"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Party must not be null");

        assertThatThrownBy(() -> Client.create(individual(), " ", "11999999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email must not be blank");

        assertThatThrownBy(() -> Client.create(individual(), "maria@example.com", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Phone must not be blank");
    }

    private Party individual() {
        return Party.create(
                "Maria",
                null,
                TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725"));
    }
}
