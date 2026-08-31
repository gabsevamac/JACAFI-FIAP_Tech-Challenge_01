package com.jacafi.tech.inventory.adapter.in.web.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.inventory.adapter.in.web.api.InventoryApi;
import com.jacafi.tech.inventory.adapter.in.web.dto.InventoryItemResponse;
import com.jacafi.tech.inventory.adapter.in.web.dto.InventoryPageResponse;
import com.jacafi.tech.inventory.adapter.in.web.dto.RegisterMaterialRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.ReplenishStockRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.ReserveMaterialRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.StockWithdrawalResponse;
import com.jacafi.tech.inventory.adapter.in.web.dto.UpdateMaterialRequest;
import com.jacafi.tech.inventory.adapter.in.web.dto.WithdrawMaterialRequest;
import com.jacafi.tech.inventory.application.service.FindInventoryItemService;
import com.jacafi.tech.inventory.application.service.ListInventoryItemsService;
import com.jacafi.tech.inventory.application.service.RegisterInventoryItemService;
import com.jacafi.tech.inventory.application.service.ReleaseInventoryReservationService;
import com.jacafi.tech.inventory.application.service.RemoveInventoryItemService;
import com.jacafi.tech.inventory.application.service.ReplenishInventoryStockService;
import com.jacafi.tech.inventory.application.service.ReserveInventoryStockService;
import com.jacafi.tech.inventory.application.service.UpdateInventoryItemService;
import com.jacafi.tech.inventory.application.service.WithdrawInventoryStockService;
import com.jacafi.tech.inventory.domain.entity.MaterialType;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.adapter.in.web.SortableFields;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.SortCriterion;

@RestController
@RequestMapping("/api/v1/inventory/items")
public class InventoryController implements InventoryApi {
    private static final SortableFields SORTABLE = SortableFields.of("id", "name", "registeredAt", "type");
    private final RegisterInventoryItemService register;
    private final FindInventoryItemService find;
    private final ListInventoryItemsService list;
    private final UpdateInventoryItemService update;
    private final RemoveInventoryItemService remove;
    private final ReplenishInventoryStockService replenish;
    private final ReserveInventoryStockService reserve;
    private final ReleaseInventoryReservationService release;
    private final WithdrawInventoryStockService withdraw;

    public InventoryController(
            RegisterInventoryItemService register,
            FindInventoryItemService find,
            ListInventoryItemsService list,
            UpdateInventoryItemService update,
            RemoveInventoryItemService remove,
            ReplenishInventoryStockService replenish,
            ReserveInventoryStockService reserve,
            ReleaseInventoryReservationService release,
            WithdrawInventoryStockService withdraw) {
        this.register = register;
        this.find = find;
        this.list = list;
        this.update = update;
        this.remove = remove;
        this.replenish = replenish;
        this.reserve = reserve;
        this.release = release;
        this.withdraw = withdraw;
    }

    @Override
    @PostMapping
    public ResponseEntity<InventoryItemResponse> register(@Valid @RequestBody RegisterMaterialRequest request) {
        var item = register.register(request.name(), request.type(), request.unitPrice(), request.initialStock());
        return ResponseEntity.created(URI.create("/api/v1/inventory/items/" + item.id()))
                .body(InventoryItemResponse.from(item));
    }

    @Override
    @GetMapping("/{id}")
    public InventoryItemResponse findById(@PathVariable UUID id) {
        return InventoryItemResponse.from(find.findById(id));
    }

    @Override
    @GetMapping
    public InventoryPageResponse list(@RequestParam(required = false) MaterialType type, PageParameters paging) {
        return InventoryPageResponse.from(
                list.list(type, pageQuery(paging)).toPageResult().map(InventoryItemResponse::from));
    }

    private static PageQuery pageQuery(PageParameters paging) {
        PageQuery query = paging.toQuery(SORTABLE);
        if (paging.sort() != null && !paging.sort().isEmpty()) {
            return query;
        }
        return new PageQuery(
                query.page(), query.size(), List.of(SortCriterion.ascending("name"), SortCriterion.ascending("id")));
    }

    @Override
    @PutMapping("/{id}")
    public InventoryItemResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateMaterialRequest request) {
        return InventoryItemResponse.from(update.update(id, request.name(), request.unitPrice()));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id) {
        remove.remove(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{id}/replenishments")
    public InventoryItemResponse replenish(@PathVariable UUID id, @Valid @RequestBody ReplenishStockRequest request) {
        return InventoryItemResponse.from(replenish.replenish(id, request.quantity()));
    }

    @Override
    @PostMapping("/{id}/reservations")
    public InventoryItemResponse reserve(@PathVariable UUID id, @Valid @RequestBody ReserveMaterialRequest request) {
        return InventoryItemResponse.from(reserve.reserve(id, request.serviceOrderId(), request.quantity()));
    }

    @Override
    @DeleteMapping("/{id}/reservations/{serviceOrderId}")
    public ResponseEntity<Void> release(@PathVariable UUID id, @PathVariable UUID serviceOrderId) {
        release.release(id, serviceOrderId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{id}/withdrawals")
    public StockWithdrawalResponse withdraw(
            @PathVariable UUID id, @Valid @RequestBody WithdrawMaterialRequest request) {
        return StockWithdrawalResponse.from(withdraw.withdraw(id, request.serviceOrderId()));
    }
}
