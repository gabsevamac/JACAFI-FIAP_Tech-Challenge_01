package com.jacafi.tech.service_order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LaunchedServiceTest {

    private static final UUID SERVICE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private Service aService() {
        return Service.create(SERVICE_ID, "Troca de óleo", new BigDecimal("120.00"));
    }

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("creates launched service with base price and computes subtotal correctly")
        void createsWithBasePrice() {
            var service = aService();

            var launched = LaunchedService.of(service, 2);

            assertThat(launched.getServiceId()).isEqualTo(SERVICE_ID);
            assertThat(launched.getServiceDescription()).isEqualTo("Troca de óleo");
            assertThat(launched.getPriceAtSale()).isEqualTo(new BigDecimal("120.00"));
            assertThat(launched.getQuantity()).isEqualTo(2);
            assertThat(launched.getSubtotal()).isEqualTo(new BigDecimal("240.00"));
        }

        @Test
        @DisplayName("creates launched service with custom frozen price at sale")
        void createsWithCustomPrice() {
            var service = aService();

            var launched = LaunchedService.of(service, new BigDecimal("100.00"), 3);

            assertThat(launched.getPriceAtSale()).isEqualTo(new BigDecimal("100.00"));
            assertThat(launched.getQuantity()).isEqualTo(3);
            assertThat(launched.getSubtotal()).isEqualTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("creates launched service directly with serviceId, description, price and quantity")
        void createsDirectly() {
            var launched = LaunchedService.of(SERVICE_ID, "Alinhamento", new BigDecimal("80.00"), 1);

            assertThat(launched.getServiceId()).isEqualTo(SERVICE_ID);
            assertThat(launched.getServiceDescription()).isEqualTo("Alinhamento");
            assertThat(launched.getPriceAtSale()).isEqualTo(new BigDecimal("80.00"));
            assertThat(launched.getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("updates quantity correctly")
        void updatesQuantity() {
            var service = aService();

            var launched = LaunchedService.of(service, 1);
            launched.updateQuantity(5);

            assertThat(launched.getQuantity()).isEqualTo(5);
            assertThat(launched.getSubtotal()).isEqualTo(new BigDecimal("600.00"));
        }

        @Test
        @DisplayName("rejects null service")
        void rejectsNullEntities() {
            assertThatNullPointerException().isThrownBy(() -> LaunchedService.of(null, 1));
        }

        @Test
        @DisplayName("rejects invalid price or quantity")
        void rejectsInvalidArguments() {
            var service = aService();

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LaunchedService.of(service, new BigDecimal("-1.00"), 1))
                    .withMessageContaining("zero or positive");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LaunchedService.of(service, new BigDecimal("10.555"), 1))
                    .withMessageContaining("two decimal places");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LaunchedService.of(service, new BigDecimal("10.00"), 0))
                    .withMessageContaining("at least 1");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LaunchedService.of(service, new BigDecimal("10.00"), -2))
                    .withMessageContaining("at least 1");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> LaunchedService.of(SERVICE_ID, "  ", new BigDecimal("10.00"), 1))
                    .withMessageContaining("blank");
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("launched services with same service id are equal")
        void equalsAndHashCode() {
            var service = aService();

            var launched1 = LaunchedService.of(service, 1);
            var launched2 = LaunchedService.of(service, 2);

            assertThat(launched1).isEqualTo(launched2);
            assertThat(launched1.hashCode()).isEqualTo(launched2.hashCode());
            assertThat(launched1).isNotEqualTo(null);
            assertThat(launched1).isNotEqualTo("other");
        }
    }
}
