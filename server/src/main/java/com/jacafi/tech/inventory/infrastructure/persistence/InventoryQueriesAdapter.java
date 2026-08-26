package com.jacafi.tech.inventory.infrastructure.persistence;

import com.jacafi.tech.inventory.application.InventoryPage;
import com.jacafi.tech.inventory.application.InventoryQueries;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the read port. Spring Data's {@code Pageable} stops here: above this class, paging is
 * expressed by the slice's own {@link InventoryPage}.
 */
@Repository
public class InventoryQueriesAdapter implements InventoryQueries {

    /** Alphabetical, which is how a person looks for a part; the id breaks ties so paging is stable. */
    private static final Sort BY_NAME = Sort.by(Sort.Direction.ASC, "name", "id");

    private final InventoryItemJpaRepository itemRepository;
    private final InventoryReservationJpaRepository reservationRepository;
    private final InventoryPersistenceMapper mapper;

    public InventoryQueriesAdapter(InventoryItemJpaRepository itemRepository,
                                   InventoryReservationJpaRepository reservationRepository,
                                   InventoryPersistenceMapper mapper) {
        this.itemRepository = itemRepository;
        this.reservationRepository = reservationRepository;
        this.mapper = mapper;
    }

    @Override
    public InventoryPage findActive(MaterialType type, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, BY_NAME);
        Page<InventoryItemJpaEntity> found = type == null
                ? itemRepository.findByRemovedAtIsNull(pageRequest)
                : itemRepository.findByTypeAndRemovedAtIsNull(type, pageRequest);

        List<InventoryItem> content = withReservations(found.getContent());
        return new InventoryPage(content, page, size, found.getTotalElements());
    }

    /**
     * Loads the reservations of a whole page in one query. Reservations are part of the aggregate,
     * so a page of items cannot be built without them; fetching them one item at a time would make
     * the cost of listing the catalogue grow with the size of the page.
     */
    private List<InventoryItem> withReservations(List<InventoryItemJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = entities.stream().map(InventoryItemJpaEntity::getId).toList();
        Map<UUID, List<InventoryReservationJpaEntity>> byItem = new HashMap<>();
        for (InventoryReservationJpaEntity row : reservationRepository.findByInventoryItemIdIn(ids)) {
            byItem.computeIfAbsent(row.getInventoryItemId(), key -> new ArrayList<>()).add(row);
        }

        return entities.stream()
                .map(entity -> mapper.toDomain(entity, byItem.getOrDefault(entity.getId(), List.of())))
                .toList();
    }
}
