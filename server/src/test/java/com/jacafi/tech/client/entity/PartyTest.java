package com.jacafi.tech.client.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartyTest {

    @Test
    void createsAnIndividualWithoutTradeName() {
        var party = Party.create(
                "  Maria da Silva  ",
                null,
                TaxIdentifier.of(PersonType.INDIVIDUAL, "529.982.247-25"));

        assertThat(party.getName()).isEqualTo("Maria da Silva");
        assertThat(party.getTradeName()).isNull();
        assertThat(party.getPersonType()).isEqualTo(PersonType.INDIVIDUAL);
    }

    @Test
    void createsALegalEntityWithTradeName() {
        var party = Party.create(
                "Oficina Jacafi Ltda",
                "  Jacafi  ",
                TaxIdentifier.of(PersonType.LEGAL_ENTITY, "00.000.000/E08G-12"));

        assertThat(party.getTradeName()).isEqualTo("Jacafi");
        assertThat(party.getPersonType()).isEqualTo(PersonType.LEGAL_ENTITY);
    }

    @Test
    void rejectsTradeNameForAnIndividual() {
        var identifier = TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725");

        assertThatThrownBy(() -> Party.create("Maria", "Loja da Maria", identifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Trade name is only allowed for legal entities");
    }

    @Test
    void updatesTheNameWithoutChangingTheFiscalIdentity() {
        var party = Party.create(
                "Maria",
                null,
                TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725"));

        party.updateName("  Maria da Silva  ", null);

        assertThat(party.getName()).isEqualTo("Maria da Silva");
        assertThat(party.getTaxIdentifier().getValue()).isEqualTo("52998224725");
    }

    @Test
    void rejectsBlankName() {
        var identifier = TaxIdentifier.of(PersonType.INDIVIDUAL, "52998224725");

        assertThatThrownBy(() -> Party.create(" ", null, identifier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Name must not be blank");
    }
}
