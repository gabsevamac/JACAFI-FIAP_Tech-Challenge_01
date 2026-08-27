package com.jacafi.tech.inventory.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.jacafi.tech.inventory.application.port.InventoryItemRepositoryPort;
import com.jacafi.tech.inventory.domain.entity.InventoryItem;
import com.jacafi.tech.inventory.domain.entity.Reservation;
import com.jacafi.tech.inventory.domain.exception.DuplicateMaterialException;

@Component
public class InventoryPersistenceAdapter implements InventoryItemRepositoryPort {
    private static final String ACTIVE_NAME_INDEX = "uk_inventory_items_active_name";
    private final InventoryItemJpaRepository items;
    private final InventoryReservationJpaRepository reservations;

    public InventoryPersistenceAdapter(
            InventoryItemJpaRepository items, InventoryReservationJpaRepository reservations) {
        this.items = items;
        this.reservations = reservations;
    }

    @Override
    public InventoryItem save(InventoryItem item, String actor) {
        try {
            InventoryItemJpaEntity entity = items.findById(item.id())
                    .map(existing -> update(existing, item, actor))
                    .orElseGet(() -> InventoryPersistenceMapper.toJpa(item));
            InventoryItemJpaEntity saved = items.saveAndFlush(entity);
            synchronizeReservations(item, actor);
            return InventoryPersistenceMapper.toDomain(
                    saved, reservations.findByInventoryItemIdAndDeletedAtIsNull(item.id()));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause().getMessage() != null
                    && exception.getMostSpecificCause().getMessage().contains(ACTIVE_NAME_INDEX))
                throw new DuplicateMaterialException();
            throw exception;
        }
    }

    @Override
    public Optional<InventoryItem> findActiveById(UUID id) {
        return items.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public Optional<InventoryItem> findActiveByIdForUpdate(UUID id) {
        return items.findForUpdateByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public boolean existsActiveWithName(String name) {
        return items.existsByNameIgnoreCaseAndDeletedAtIsNull(name);
    }

    @Override
    public boolean existsActiveWithNameExcluding(String name, UUID id) {
        return items.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(name, id);
    }

    private InventoryItem toDomain(InventoryItemJpaEntity entity) {
        return InventoryPersistenceMapper.toDomain(
                entity, reservations.findByInventoryItemIdAndDeletedAtIsNull(entity.id()));
    }

    private static InventoryItemJpaEntity update(InventoryItemJpaEntity entity, InventoryItem item, String actor) {
        if (entity.getVersion() != item.version())
            throw new IllegalStateException("Inventory item was changed by another transaction");
        entity.apply(item, actor);
        return entity;
    }

    private void synchronizeReservations(InventoryItem item, String actor) {
        Map<UUID, InventoryReservationJpaEntity> stored = new HashMap<>();
        for (InventoryReservationJpaEntity row : reservations.findByInventoryItemIdAndDeletedAtIsNull(item.id()))
            stored.put(row.id(), row);
        List<Reservation> open = item.reservations();
        for (InventoryReservationJpaEntity row : stored.values()) {
            if (open.stream().noneMatch(reservation -> reservation.id().equals(row.id())))
                row.release(item.updatedAt(), actor);
        }
        reservations.saveAll(stored.values());
        reservations.flush();
        for (Reservation reservation : open) {
            InventoryReservationJpaEntity existing = stored.get(reservation.id());
            if (existing == null) {
                reservations.save(InventoryPersistenceMapper.toJpa(item, reservation));
            } else if (existing.quantity() != reservation.quantity().value()) {
                existing.changeQuantity(reservation.quantity().value());
            }
        }
        reservations.flush();
    }
}
