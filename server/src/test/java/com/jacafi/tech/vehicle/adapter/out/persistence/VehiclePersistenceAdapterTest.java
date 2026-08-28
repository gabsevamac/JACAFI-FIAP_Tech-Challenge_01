package com.jacafi.tech.vehicle.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.jacafi.tech.vehicle.domain.entity.LicensePlate;
import com.jacafi.tech.vehicle.domain.entity.Vehicle;
import com.jacafi.tech.vehicle.domain.exception.VehicleUpdateConflictException;

@ExtendWith(MockitoExtension.class)
class VehiclePersistenceAdapterTest {

    @Mock
    private VehicleJpaRepository repository;

    @Test
    void rejectsAStaleAggregateBeforeItCanOverwriteTheManagedRow() {
        UUID id = UUID.randomUUID();
        VehicleJpaEntity managed = new VehicleJpaEntity(id, "ABC1D23", "Volkswagen", "Gol", 2020, UUID.randomUUID());
        ReflectionTestUtils.setField(managed, "version", 2L);
        Vehicle stale = Vehicle.restore(
                id,
                new LicensePlate("ABC1D23"),
                "Ford",
                "Ka",
                2020,
                managed.customerId(),
                1,
                Instant.EPOCH,
                Instant.EPOCH,
                null);
        when(repository.findById(id)).thenReturn(Optional.of(managed));

        assertThatThrownBy(() -> new VehiclePersistenceAdapter(repository).save(stale, "advisor"))
                .isInstanceOf(VehicleUpdateConflictException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void logicalRemovalStoresTheAuthenticatedActorWithTheDeletionTimestamp() {
        UUID customerId = UUID.randomUUID();
        VehicleJpaEntity entity =
                new VehicleJpaEntity(UUID.randomUUID(), "ABC1D23", "Volkswagen", "Gol", 2020, customerId);
        Vehicle removed = Vehicle.register(
                entity.id(),
                new LicensePlate("ABC1D23"),
                "Volkswagen",
                "Gol",
                2020,
                customerId,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        removed.remove(Instant.parse("2026-08-27T10:00:00Z"));

        assertThatThrownBy(() -> entity.apply(removed, "")).isInstanceOf(IllegalArgumentException.class);
        entity.apply(removed, "advisor");

        assertThat(entity.getDeletedAt()).contains(Instant.parse("2026-08-27T10:00:00Z"));
        assertThat(entity.getDeletedBy()).contains("advisor");
    }
}
