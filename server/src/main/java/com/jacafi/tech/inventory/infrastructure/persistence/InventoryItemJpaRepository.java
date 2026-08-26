package com.jacafi.tech.inventory.infrastructure.persistence;

import com.jacafi.tech.inventory.domain.MaterialType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository over the storage shape.
 *
 * <p>Every derived query carries {@code AndRemovedAtIsNull}: a removed item keeps its row so the
 * ledger still points at something, and answers nothing. This interface is an implementation
 * detail of the adapters in this package — the application layer talks to the domain port instead.
 */
interface InventoryItemJpaRepository extends JpaRepository<InventoryItemJpaEntity, UUID> {

    Optional<InventoryItemJpaEntity> findByIdAndRemovedAtIsNull(UUID id);

    /**
     * Same row, held until the transaction ends: {@code SELECT ... FOR UPDATE}.
     *
     * <p>Every write path in this slice goes through here. Reserving reads the balance and then
     * writes a decision based on it, and without the lock two callers reserving the last unit
     * would both read one available and both succeed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItemJpaEntity> findForUpdateByIdAndRemovedAtIsNull(UUID id);

    boolean existsByNameIgnoreCaseAndRemovedAtIsNull(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndRemovedAtIsNull(String name, UUID id);

    Page<InventoryItemJpaEntity> findByRemovedAtIsNull(Pageable pageable);

    Page<InventoryItemJpaEntity> findByTypeAndRemovedAtIsNull(MaterialType type, Pageable pageable);
}
