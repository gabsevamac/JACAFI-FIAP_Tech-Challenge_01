package com.jacafi.tech.inventory.api;

import com.jacafi.tech.inventory.api.dto.InventoryItemResponse;
import com.jacafi.tech.inventory.api.dto.InventoryPageResponse;
import com.jacafi.tech.inventory.api.dto.RegisterMaterialRequest;
import com.jacafi.tech.inventory.api.dto.ReplenishStockRequest;
import com.jacafi.tech.inventory.api.dto.ReserveMaterialRequest;
import com.jacafi.tech.inventory.api.dto.StockWithdrawalResponse;
import com.jacafi.tech.inventory.api.dto.UpdateMaterialRequest;
import com.jacafi.tech.inventory.api.dto.WithdrawMaterialRequest;
import com.jacafi.tech.inventory.application.FindInventoryItemUseCase;
import com.jacafi.tech.inventory.application.ListInventoryUseCase;
import com.jacafi.tech.inventory.application.RegisterMaterialCommand;
import com.jacafi.tech.inventory.application.RegisterMaterialUseCase;
import com.jacafi.tech.inventory.application.ReleaseReservationUseCase;
import com.jacafi.tech.inventory.application.RemoveMaterialUseCase;
import com.jacafi.tech.inventory.application.ReplenishStockCommand;
import com.jacafi.tech.inventory.application.ReplenishStockUseCase;
import com.jacafi.tech.inventory.application.ReserveMaterialCommand;
import com.jacafi.tech.inventory.application.ReserveMaterialUseCase;
import com.jacafi.tech.inventory.application.UpdateMaterialCommand;
import com.jacafi.tech.inventory.application.UpdateMaterialUseCase;
import com.jacafi.tech.inventory.application.WithdrawMaterialUseCase;
import com.jacafi.tech.inventory.domain.InventoryItem;
import com.jacafi.tech.inventory.domain.MaterialType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST surface of the inventory slice. Every endpoint requires a JWT.
 *
 * <p>Thin on purpose: it turns HTTP into a command, delegates, and turns the result into a DTO.
 * The rules live in the aggregate, the policies in the use cases, the mapping of a failure onto a
 * status code in {@link InventoryExceptionHandler}, and the API description in {@link InventoryApi}.
 *
 * <p>The author of every write comes from the authenticated principal, never from the request
 * body: a client that could name its own actor would make the ledger worthless as evidence of who
 * moved what.
 *
 * <p>Reservations and withdrawals have endpoints of their own because the MVP has no policy
 * engine yet. On the board they are triggered by {@code WhenEstimateApproved},
 * {@code WhenEstimateRejected} and {@code WhenServicesCompleted}, reacting to events from the
 * service order slice — never by a direct call from it. When those policies are wired, they enter
 * through the same use cases, and these endpoints stay as the manual correction path a stockroom
 * always needs.
 *
 * <p>TODO: the board names the Manager as the actor for registering a material and replenishing
 * stock. The role model currently holds only USER and ADMIN, neither of which is a manager, so
 * every authenticated employee can do both. Restricting them belongs with the role that names
 * them, not with a guess about which existing role is closest.
 */
@RestController
@RequestMapping("/api/v1/inventory/items")
public class InventoryController implements InventoryApi {

    /** Cap on page size, so a caller cannot ask for the whole catalogue in one request. */
    private static final int MAX_PAGE_SIZE = 100;

    private final RegisterMaterialUseCase registerMaterial;
    private final UpdateMaterialUseCase updateMaterial;
    private final RemoveMaterialUseCase removeMaterial;
    private final ReplenishStockUseCase replenishStock;
    private final ReserveMaterialUseCase reserveMaterial;
    private final ReleaseReservationUseCase releaseReservation;
    private final WithdrawMaterialUseCase withdrawMaterial;
    private final FindInventoryItemUseCase findInventoryItem;
    private final ListInventoryUseCase listInventory;

    public InventoryController(RegisterMaterialUseCase registerMaterial,
                               UpdateMaterialUseCase updateMaterial,
                               RemoveMaterialUseCase removeMaterial,
                               ReplenishStockUseCase replenishStock,
                               ReserveMaterialUseCase reserveMaterial,
                               ReleaseReservationUseCase releaseReservation,
                               WithdrawMaterialUseCase withdrawMaterial,
                               FindInventoryItemUseCase findInventoryItem,
                               ListInventoryUseCase listInventory) {
        this.registerMaterial = registerMaterial;
        this.updateMaterial = updateMaterial;
        this.removeMaterial = removeMaterial;
        this.replenishStock = replenishStock;
        this.reserveMaterial = reserveMaterial;
        this.releaseReservation = releaseReservation;
        this.withdrawMaterial = withdrawMaterial;
        this.findInventoryItem = findInventoryItem;
        this.listInventory = listInventory;
    }

    @Override
    @PostMapping
    public ResponseEntity<InventoryItemResponse> register(@Valid @RequestBody RegisterMaterialRequest request,
                                                          Authentication authentication) {
        InventoryItem item = registerMaterial.register(new RegisterMaterialCommand(request.name(),
                request.type(),
                request.unitPrice(),
                request.initialQuantity(),
                authentication.getName()));

        URI location = UriComponentsBuilder.fromPath("/api/v1/inventory/items/{id}")
                .buildAndExpand(item.getId())
                .toUri();
        return ResponseEntity.created(location).body(InventoryItemResponse.from(item));
    }

    @Override
    @GetMapping("/{id}")
    public InventoryItemResponse findById(@PathVariable UUID id) {
        return InventoryItemResponse.from(findInventoryItem.byId(id));
    }

    @Override
    @GetMapping
    public InventoryPageResponse list(@RequestParam(required = false) MaterialType type,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return InventoryPageResponse.from(listInventory.list(type, page, size));
    }

    @Override
    @PutMapping("/{id}")
    public InventoryItemResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateMaterialRequest request,
                                        Authentication authentication) {
        return InventoryItemResponse.from(updateMaterial.update(new UpdateMaterialCommand(id,
                request.name(),
                request.unitPrice(),
                authentication.getName())));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id, Authentication authentication) {
        removeMaterial.remove(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{id}/replenishments")
    public InventoryItemResponse replenish(@PathVariable UUID id,
                                           @Valid @RequestBody ReplenishStockRequest request,
                                           Authentication authentication) {
        return InventoryItemResponse.from(replenishStock.replenish(
                new ReplenishStockCommand(id, request.quantity(), authentication.getName())));
    }

    /**
     * Answers with the whole item rather than with the reservation alone. What the caller needs to
     * know next is how much is left available, and that is a property of the item.
     */
    @Override
    @PostMapping("/{id}/reservations")
    public InventoryItemResponse reserve(@PathVariable UUID id,
                                         @Valid @RequestBody ReserveMaterialRequest request,
                                         Authentication authentication) {
        return InventoryItemResponse.from(reserveMaterial.reserve(new ReserveMaterialCommand(id,
                request.serviceOrderId(),
                request.quantity(),
                authentication.getName())));
    }

    @Override
    @DeleteMapping("/{id}/reservations/{serviceOrderId}")
    public ResponseEntity<Void> release(@PathVariable UUID id,
                                        @PathVariable UUID serviceOrderId,
                                        Authentication authentication) {
        releaseReservation.release(id, serviceOrderId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{id}/withdrawals")
    public StockWithdrawalResponse withdraw(@PathVariable UUID id,
                                            @Valid @RequestBody WithdrawMaterialRequest request,
                                            Authentication authentication) {
        return StockWithdrawalResponse.from(
                withdrawMaterial.withdraw(id, request.serviceOrderId(), authentication.getName()));
    }
}
