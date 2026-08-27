package com.jacafi.tech.service_order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServiceTest {

    private static final UUID SERVICE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("creates a valid service with normalized description and scaled base price")
        void createsService() {
            var service = Service.create("  Troca   de  óleo ", new BigDecimal("150.00"));

            assertThat(service.getDescription()).isEqualTo("Troca de óleo");
            assertThat(service.getBasePrice()).isEqualTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("creates a service with explicit id")
        void createsServiceWithExplicitId() {
            var service = Service.create(SERVICE_ID, "Alinhamento", new BigDecimal("80.00"));

            assertThat(service.getId()).isEqualTo(SERVICE_ID);
            assertThat(service.getDescription()).isEqualTo("Alinhamento");
            assertThat(service.getBasePrice()).isEqualTo(new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("rejects null or blank description")
        void rejectsBlankDescription() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Service.create("   ", new BigDecimal("100.00")))
                    .withMessageContaining("blank");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Service.create(null, new BigDecimal("100.00")))
                    .withMessageContaining("blank");
        }

        @Test
        @DisplayName("rejects description exceeding max length (45 chars)")
        void rejectsLongDescription() {
            var longDesc = "A".repeat(46);
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Service.create(longDesc, new BigDecimal("100.00")))
                    .withMessageContaining("maximum length");
        }

        @Test
        @DisplayName("rejects null or negative base price")
        void rejectsInvalidBasePrice() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Service.create("Balanceamento", null))
                    .withMessageContaining("null");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Service.create("Balanceamento", new BigDecimal("-10.00")))
                    .withMessageContaining("zero or positive");
        }

        @Test
        @DisplayName("rejects base price with more than 2 decimal places")
        void rejectsInvalidScale() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> Service.create("Balanceamento", new BigDecimal("100.555")))
                    .withMessageContaining("two decimal places");
        }

        @Test
        @DisplayName("rejects null explicit id")
        void rejectsNullId() {
            assertThatNullPointerException()
                    .isThrownBy(() -> Service.create(null, "Troca de óleo", new BigDecimal("150.00")));
        }
    }

    @Nested
    @DisplayName("updates")
    class Updates {

        @Test
        @DisplayName("updates description and base price successfully")
        void updatesDescriptionAndPrice() {
            var service = Service.create("Troca de óleo", new BigDecimal("150.00"));
            service.update("Troca de óleo sintético", new BigDecimal("190.00"));

            assertThat(service.getDescription()).isEqualTo("Troca de óleo sintético");
            assertThat(service.getBasePrice()).isEqualTo(new BigDecimal("190.00"));
        }

        @Test
        @DisplayName("updates base price only")
        void updatesBasePrice() {
            var service = Service.create("Troca de óleo", new BigDecimal("150.00"));
            service.updateBasePrice(new BigDecimal("175.50"));

            assertThat(service.getBasePrice()).isEqualTo(new BigDecimal("175.50"));
        }

        @Test
        @DisplayName("updates description only")
        void updatesDescription() {
            var service = Service.create("Troca de óleo", new BigDecimal("150.00"));
            service.updateDescription("Troca de óleo mineral");

            assertThat(service.getDescription()).isEqualTo("Troca de óleo mineral");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("services with same id are equal")
        void equalsAndHashCode() {
            var service1 = Service.create(SERVICE_ID, "Troca de óleo", new BigDecimal("150.00"));
            var service2 = Service.create(SERVICE_ID, "Outro nome", new BigDecimal("200.00"));
            var service3 = Service.create(UUID.randomUUID(), "Troca de óleo", new BigDecimal("150.00"));

            assertThat(service1).isEqualTo(service2);
            assertThat(service1).hasSameHashCodeAs(service2);
            assertThat(service1).isNotEqualTo(service3);
            assertThat(service1).isNotEqualTo(null);
            assertThat(service1).isNotEqualTo("some string");
        }
    }
}
