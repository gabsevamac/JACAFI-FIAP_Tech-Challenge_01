package com.jacafi.tech.service_order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LaunchedOrderTest {

    private static final UUID SERVICE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("creates valid LaunchedOrder and calculates subtotal correctly")
    void createsLaunchedOrder() {
        var order = LaunchedOrder.of(SERVICE_ID, "Troca de óleo", new BigDecimal("120.00"), 3);

        assertThat(order.serviceId()).isEqualTo(SERVICE_ID);
        assertThat(order.description()).isEqualTo("Troca de óleo");
        assertThat(order.priceAtSale()).isEqualTo(new BigDecimal("120.00"));
        assertThat(order.quantity()).isEqualTo(3);
        assertThat(order.getSubtotal()).isEqualTo(new BigDecimal("360.00"));
    }

    @Test
    @DisplayName("creates LaunchedOrder from LaunchedService entity")
    void createsFromLaunchedService() {
        var so = ServiceOrder.open(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CLOCK);
        var service = Service.create(SERVICE_ID, "Alinhamento", new BigDecimal("90.00"));
        var ls = LaunchedService.of(service, 2);

        var launchedOrder = LaunchedOrder.from(ls);

        assertThat(launchedOrder.serviceId()).isEqualTo(SERVICE_ID);
        assertThat(launchedOrder.description()).isEqualTo("Alinhamento");
        assertThat(launchedOrder.priceAtSale()).isEqualTo(new BigDecimal("90.00"));
        assertThat(launchedOrder.quantity()).isEqualTo(2);
        assertThat(launchedOrder.getSubtotal()).isEqualTo(new BigDecimal("180.00"));
    }

    @Test
    @DisplayName("validates invariants")
    void validatesInvariants() {
        assertThatNullPointerException().isThrownBy(() -> LaunchedOrder.of(null, "Desc", new BigDecimal("10.00"), 1));

        assertThatNullPointerException()
                .isThrownBy(() -> LaunchedOrder.of(SERVICE_ID, null, new BigDecimal("10.00"), 1));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchedOrder.of(SERVICE_ID, "  ", new BigDecimal("10.00"), 1));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchedOrder.of(SERVICE_ID, "Desc", new BigDecimal("-1.00"), 1));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchedOrder.of(SERVICE_ID, "Desc", new BigDecimal("10.555"), 1));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> LaunchedOrder.of(SERVICE_ID, "Desc", new BigDecimal("10.00"), 0));
    }
}
