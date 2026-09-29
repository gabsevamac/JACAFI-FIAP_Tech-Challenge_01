package com.jacafi.tech.inventory.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.inventory.application.port.InventoryAuditLedgerPort;
import com.jacafi.tech.inventory.application.port.InventoryItemRepository;
import com.jacafi.tech.inventory.application.port.InventoryQueryPort;
import com.jacafi.tech.inventory.application.service.FindInventoryItemService;
import com.jacafi.tech.inventory.application.service.InventoryAccessPolicy;
import com.jacafi.tech.inventory.application.service.ListInventoryItemsService;
import com.jacafi.tech.inventory.application.service.RegisterInventoryItemService;
import com.jacafi.tech.inventory.application.service.ReleaseInventoryReservationService;
import com.jacafi.tech.inventory.application.service.RemoveInventoryItemService;
import com.jacafi.tech.inventory.application.service.ReplenishInventoryStockService;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.inventory.application.service.UpdateInventoryItemService;
import com.jacafi.tech.inventory.application.service.WithdrawInventoryStockService;
import com.jacafi.tech.shared.application.AuditTrailPort;

@Configuration
public class InventoryConfiguration {
    @Bean
    InventoryAccessPolicy inventoryAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new InventoryAccessPolicy(currentUser);
    }

    @Bean
    RegisterInventoryItemService registerInventoryItemService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new RegisterInventoryItemService(items, ledger, audit, access, clock);
    }

    @Bean
    FindInventoryItemService findInventoryItemService(InventoryItemRepository items, InventoryAccessPolicy access) {
        return new FindInventoryItemService(items, access);
    }

    @Bean
    ListInventoryItemsService listInventoryItemsService(InventoryQueryPort query, InventoryAccessPolicy access) {
        return new ListInventoryItemsService(query, access);
    }

    @Bean
    UpdateInventoryItemService updateInventoryItemService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new UpdateInventoryItemService(items, ledger, audit, access, clock);
    }

    @Bean
    RemoveInventoryItemService removeInventoryItemService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new RemoveInventoryItemService(items, ledger, audit, access, clock);
    }

    @Bean
    ReplenishInventoryStockService replenishInventoryStockService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new ReplenishInventoryStockService(items, ledger, audit, access, clock);
    }

    @Bean
    ReserveInventoryStockService reserveInventoryStockService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new ReserveInventoryStockService(items, ledger, audit, access, clock);
    }

    @Bean
    ReleaseInventoryReservationService releaseInventoryReservationService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new ReleaseInventoryReservationService(items, ledger, audit, access, clock);
    }

    @Bean
    WithdrawInventoryStockService withdrawInventoryStockService(
            InventoryItemRepository items,
            InventoryAuditLedgerPort ledger,
            AuditTrailPort audit,
            InventoryAccessPolicy access,
            Clock clock) {
        return new WithdrawInventoryStockService(items, ledger, audit, access, clock);
    }
}
