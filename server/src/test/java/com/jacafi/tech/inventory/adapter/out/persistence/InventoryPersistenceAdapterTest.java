package com.jacafi.tech.inventory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.util.ReflectionTestUtils;

import com.jacafi.tech.inventory.domain.entity.AuditedOperation;
import com.jacafi.tech.inventory.domain.entity.InventoryAuditEntry;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.inventory.domain.entity.Stock;

class InventoryPersistenceAdapterTest {
    @Test
    void writeLookupUsesPessimisticWriteLock() throws Exception {
        assertThat(InventoryItemJpaRepository.class
                        .getMethod("findForUpdateByIdAndDeletedAtIsNull", UUID.class)
                        .getAnnotation(Lock.class)
                        .value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void ledgerMapsAppendOnlyMovementColumns() {
        InventoryAuditJpaRepository repository = Mockito.mock(InventoryAuditJpaRepository.class);
        UUID itemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        new JpaInventoryAuditLedger(repository)
                .append(InventoryAuditEntry.movement(
                        itemId,
                        AuditedOperation.RESERVED,
                        orderId,
                        new com.jacafi.tech.inventory.domain.entity.Stock(2),
                        "advisor",
                        Instant.EPOCH));
        ArgumentCaptor<InventoryAuditEntryJpaEntity> entry =
                ArgumentCaptor.forClass(InventoryAuditEntryJpaEntity.class);
        verify(repository).save(entry.capture());
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "inventoryItemId"))
                .isEqualTo(itemId);
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "serviceOrderId"))
                .isEqualTo(orderId);
        assertThat(ReflectionTestUtils.getField(entry.getValue(), "quantity")).isEqualTo(2);
    }

    @Test
    void appliesLogicalRemovalWithTheAuthenticatedActor() {
        Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        InventoryItem item = InventoryItem.register(
                UUID.randomUUID(), "Oil filter", MaterialType.PART, java.math.BigDecimal.ONE, Stock.ZERO, clock);
        item.remove(clock);
        InventoryItemJpaEntity entity =
                new InventoryItemJpaEntity(item.id(), item.name(), item.type(), item.unitPrice(), 0);

        entity.apply(item, "advisor");

        assertThat(entity.getDeletedBy()).contains("advisor");
        assertThat(entity.getDeletedAt()).contains(Instant.EPOCH);
    }
}
