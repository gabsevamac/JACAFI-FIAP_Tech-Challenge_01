package com.jacafi.tech.inventory.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.jacafi.tech.inventory.domain.entity.StockWithdrawal;

public record StockWithdrawalResponse(UUID inventoryItemId, UUID serviceOrderId, int quantity, Instant withdrawnAt) {
    public static StockWithdrawalResponse from(StockWithdrawal withdrawal) {
        return new StockWithdrawalResponse(
                withdrawal.inventoryItemId(),
                withdrawal.serviceOrderId(),
                withdrawal.quantity().value(),
                withdrawal.withdrawnAt());
    }
}
