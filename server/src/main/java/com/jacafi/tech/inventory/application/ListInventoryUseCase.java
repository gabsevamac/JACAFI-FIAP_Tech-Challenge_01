package com.jacafi.tech.inventory.application;

import com.jacafi.tech.inventory.domain.MaterialType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists the catalogue a page at a time, optionally narrowed to parts or to supplies.
 *
 * <p>Thin by nature: there is no rule to enforce and nothing to decide, which is why it delegates
 * straight to the read port. It exists so the api layer depends on the application layer for every
 * operation, rather than on a port for this one and on use cases for the others.
 */
@Service
public class ListInventoryUseCase {

    private final InventoryQueries queries;

    public ListInventoryUseCase(InventoryQueries queries) {
        this.queries = queries;
    }

    /** @param type null to list parts and supplies together */
    @Transactional(readOnly = true)
    public InventoryPage list(MaterialType type, int page, int size) {
        return queries.findActive(type, page, size);
    }
}
