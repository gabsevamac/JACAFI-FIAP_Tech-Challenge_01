package com.jacafi.tech.inventory.api.dto;

import com.jacafi.tech.inventory.domain.StockWithdrawal;

import java.time.Instant;
import java.util.UUID;

/** A baixa as the API exposes it: what left, for which order, and when. */
public record StockWithdrawalResponse(UUID inventoryItemId,
                                      UUID serviceOrderId,
                                      int quantity,
                                      Instant withdrawnAt) {

    public static StockWithdrawalResponse from(StockWithdrawal withdrawal) {
        return new StockWithdrawalResponse(withdrawal.inventoryItemId(),
                withdrawal.serviceOrderId(),
                withdrawal.quantity().value(),
                withdrawal.withdrawnAt());
    }
}
