package com.jacafi.tech.inventory.adapter.in.web.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.inventory.adapter.in.web.dto.InventoryItemResponse;
import com.jacafi.tech.inventory.adapter.in.web.dto.InventoryPageResponse;
import com.jacafi.tech.inventory.adapter.in.web.dto.RegisterMaterialRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.ReplenishStockRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.ReserveMaterialRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.StockWithdrawalResponse;
import com.jacafi.tech.inventory.adapter.in.web.dto.UpdateMaterialRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.WithdrawMaterialRequest;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Inventory", description = "Parts, supplies, stock and reservations")
@SecurityRequirement(name = "bearer-jwt")
public interface InventoryApi {
    ResponseEntity<InventoryItemResponse> register(RegisterMaterialRequest request);

    InventoryItemResponse findById(UUID id);

    InventoryPageResponse list(MaterialType type, PageParameters paging);

    InventoryItemResponse update(UUID id, UpdateMaterialRequest request);

    ResponseEntity<Void> remove(UUID id);

    InventoryItemResponse replenish(UUID id, ReplenishStockRequest request);

    InventoryItemResponse reserve(UUID id, ReserveMaterialRequest request);

    ResponseEntity<Void> release(UUID id, UUID serviceOrderId);

    StockWithdrawalResponse withdraw(UUID id, WithdrawMaterialRequest request);
}
