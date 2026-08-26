package com.jacafi.tech.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Also covers what used to be PartyTest: with Party collapsed into this entity, the name and trade
 * name rules live here.
 */
class CustomerTest {

    private static final TaxId CPF = TaxId.of("52998224725");
    private static final TaxId CNPJ = TaxId.of("00.000.000/E08G-12");

    private static Customer individual() {
        return Customer.create(CPF, "Maria", null, "maria@example.com", "11999999999");
    }

    @Nested
    class Creation {

        @Test
        void startsActiveAndTrimsContactInformation() {
            var customer = Customer.create(CPF, "Maria", null, " maria@example.com ", " 11999999999 ");

            assertThat(customer.isActive()).isTrue();
            assertThat(customer.getEmail()).isEqualTo("maria@example.com");
            assertThat(customer.getPhone()).isEqualTo("11999999999");
            assertThat(customer.getTaxId()).isEqualTo(CPF);
        }

        @Test
        void trimsTheName() {
            var customer = Customer.create(CPF, "  Maria da Silva  ", null, "m@example.com", "1199");

            assertThat(customer.getName()).isEqualTo("Maria da Silva");
            assertThat(customer.getTradeName()).isNull();
        }

        @Test
        @DisplayName("a legal entity may carry a trade name")
        void acceptsATradeNameForALegalEntity() {
            var customer =
                    Customer.create(CNPJ, "Oficina Jacafi Ltda", "  Jacafi  ", "contato@jacafi.com", "1133333333");

            assertThat(customer.getTradeName()).isEqualTo("Jacafi");
        }

        @Test
        @DisplayName("a natural person may not, and the type of the registration is what says so")
        void rejectsATradeNameForAnIndividual() {
            assertThatThrownBy(() -> Customer.create(CPF, "Maria", "Loja da Maria", "maria@example.com", "11999999999"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Trade name is only allowed for legal entities");
        }

        @Test
        void rejectsMissingRegistrationOrContactInformation() {
            assertThatThrownBy(() -> Customer.create(null, "Maria", null, "maria@example.com", "1199"))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Tax id must not be null");

            assertThatThrownBy(() -> Customer.create(CPF, "Maria", null, " ", "11999999999"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email must not be blank");

            assertThatThrownBy(() -> Customer.create(CPF, "Maria", null, "maria@example.com", " "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Phone must not be blank");
        }

        @Test
        void rejectsABlankName() {
            assertThatThrownBy(() -> Customer.create(CPF, " ", null, "maria@example.com", "1199"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Name must not be blank");
        }
    }

    @Nested
    class Changes {

        @Test
        @DisplayName("the name changes; the fiscal identity does not")
        void updatesTheNameWithoutTouchingTheRegistration() {
            var customer = individual();

            customer.updateName("  Maria da Silva  ", null);

            assertThat(customer.getName()).isEqualTo("Maria da Silva");
            assertThat(customer.getTaxId()).isEqualTo(CPF);
        }

        @Test
        void updatesContactInformation() {
            var customer = individual();

            customer.updateContactInformation("novo@example.com", "11888888888");

            assertThat(customer.getEmail()).isEqualTo("novo@example.com");
            assertThat(customer.getPhone()).isEqualTo("11888888888");
        }

        @Test
        @DisplayName("the trade name rule still applies on update")
        void rejectsATradeNameForAnIndividualOnUpdate() {
            var customer = individual();

            assertThatThrownBy(() -> customer.updateName("Maria", "Loja da Maria"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void deactivatesIdempotently() {
            var customer = individual();

            customer.deactivate();
            customer.deactivate();

            assertThat(customer.isActive()).isFalse();
        }
    }

    @Test
    @DisplayName("toString carries no personal data beyond a masked registration")
    void toStringDoesNotLeak() {
        var customer = individual();

        assertThat(customer.toString())
                .doesNotContain("52998224725")
                .doesNotContain("Maria")
                .doesNotContain("maria@example.com")
                .doesNotContain("11999999999")
                .contains("********725");
    }
}
