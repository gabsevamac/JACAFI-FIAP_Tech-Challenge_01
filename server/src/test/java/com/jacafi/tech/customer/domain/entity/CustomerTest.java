package com.jacafi.tech.customer.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CustomerTest {

    @Test
    void registersAnActiveCustomerWithNormalizedContactData() {
        Customer customer = Customer.register(
                TaxId.of("52998224725"), " Maria da Silva ", null, " maria@example.com ", " 11999999999 ");

        assertThat(customer.id()).isNotNull();
        assertThat(customer.active()).isTrue();
        assertThat(customer.version()).isZero();
        assertThat(customer.name()).isEqualTo("Maria da Silva");
        assertThat(customer.email()).isEqualTo("maria@example.com");
    }

    @Test
    void allowsTradeNameOnlyForLegalEntities() {
        Customer legalEntity = Customer.register(
                TaxId.of("12.ABC.345/01DE-35"), "Jacafi Ltda", "Jacafi", "contato@jacafi.com", "1133334444");

        assertThat(legalEntity.tradeName()).isEqualTo("Jacafi");
        assertThatThrownBy(() -> Customer.register(
                        TaxId.of("52998224725"), "Maria", "Loja da Maria", "maria@example.com", "11999999999"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesOnlyMutableProfileDataAndMasksPersonalDataInLogs() {
        Customer customer =
                Customer.register(TaxId.of("52998224725"), "Maria", null, "maria@example.com", "11999999999");

        customer.changeProfile("Maria da Silva", null, "novo@example.com", "11888888888");
        customer.deactivate();

        assertThat(customer.taxId().value()).isEqualTo("52998224725");
        assertThat(customer.name()).isEqualTo("Maria da Silva");
        assertThat(customer.active()).isFalse();
        assertThat(customer.toString())
                .doesNotContain("Maria")
                .doesNotContain("novo@example.com")
                .doesNotContain("52998224725");
    }
}
