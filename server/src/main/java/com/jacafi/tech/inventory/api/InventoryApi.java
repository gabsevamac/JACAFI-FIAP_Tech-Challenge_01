package com.jacafi.tech.inventory.api;

import com.jacafi.tech.inventory.api.dto.InventoryItemResponse;
import com.jacafi.tech.inventory.api.dto.InventoryPageResponse;
import com.jacafi.tech.inventory.api.dto.RegisterMaterialRequest;
import com.jacafi.tech.inventory.api.dto.ReplenishStockRequest;
import com.jacafi.tech.inventory.api.dto.ReserveMaterialRequest;
import com.jacafi.tech.inventory.api.dto.StockWithdrawalResponse;
import com.jacafi.tech.inventory.api.dto.UpdateMaterialRequest;
import com.jacafi.tech.inventory.api.dto.WithdrawMaterialRequest;
import com.jacafi.tech.inventory.domain.MaterialType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Documented contract of the inventory endpoints.
 *
 * <p>The OpenAPI annotations live here rather than on the controller, and the split is by purpose:
 * this file describes the API to whoever consumes it, while the controller decides how a request
 * becomes a call. Mixed together, the documentation outweighs the code it describes and every
 * change to either means reading past the other.
 *
 * <p>Only the description belongs here. Spring's own annotations — the mapping, the path
 * variables, the request body, the validation trigger — stay on the implementation, where the
 * framework expects them.
 *
 * <p>Not a port and not an abstraction: it has exactly one implementation and will never have
 * another. It is a place to put annotations.
 */
@Tag(name = "Inventory", description = "Parts and supplies, their stock and their reservations")
@SecurityRequirement(name = "bearer-jwt")
public interface InventoryApi {

    @Operation(summary = "Register a part or a supply",
            description = "The name is normalized before the uniqueness check, so \"Filtro de "
                    + "óleo\" and \"filtro  de óleo\" are the same material. A name already held "
                    + "by an active item is rejected with 409. The material type cannot be changed "
                    + "afterwards.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registered"),
            @ApiResponse(responseCode = "400", description = "Attribute missing or out of range",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "409", description = "Name already registered to an active material",
                    content = @Content)
    })
    ResponseEntity<InventoryItemResponse> register(RegisterMaterialRequest request,
                                                   Authentication authentication);

    @Operation(summary = "Read one item, with its balance and its open reservations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item with this identifier",
                    content = @Content)
    })
    InventoryItemResponse findById(@Parameter(description = "Identifier assigned at registration") UUID id);

    @Operation(summary = "List the catalogue",
            description = "Alphabetical by name. Omit the type to list parts and supplies together.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listed"),
            @ApiResponse(responseCode = "400", description = "Page or size out of range", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    InventoryPageResponse list(@Parameter(description = "PART, SUPPLY, or omitted for both") MaterialType type,
                               @Parameter(description = "Zero-based page number") int page,
                               @Parameter(description = "Page size, at most 100") int size);

    @Operation(summary = "Correct the name and the unit price",
            description = "The material type cannot be changed, and neither can the balance: stock "
                    + "moves only through replenishment, reservation and withdrawal, each of which "
                    + "leaves a line in the ledger.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Attribute missing or out of range",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item with this identifier",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Name already registered to another active material",
                    content = @Content)
    })
    InventoryItemResponse update(@Parameter(description = "Identifier of the item to correct") UUID id,
                                 UpdateMaterialRequest request,
                                 Authentication authentication);

    @Operation(summary = "Take a material out of the catalogue",
            description = "The row is not deleted: withdrawals already recorded point at it. The "
                    + "item stops answering queries and stops accepting operations. An item with "
                    + "open reservations is refused with 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item with this identifier",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "The item still holds open reservations",
                    content = @Content)
    })
    ResponseEntity<Void> remove(@Parameter(description = "Identifier of the item to remove") UUID id,
                                Authentication authentication);

    @Operation(summary = "Add units to the shelf",
            description = "Raises what is on hand and, with it, what is available. Reservations "
                    + "already held are untouched.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Replenished"),
            @ApiResponse(responseCode = "400", description = "Quantity out of range", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item with this identifier",
                    content = @Content)
    })
    InventoryItemResponse replenish(@Parameter(description = "Identifier of the item") UUID id,
                                    ReplenishStockRequest request,
                                    Authentication authentication);

    @Operation(summary = "Hold units for a service order",
            description = "Nothing leaves the shelf: the units stop being available to any other "
                    + "order. Reserving twice for the same order enlarges the reservation it "
                    + "already holds — the additional repair of the board. Asking for more than is "
                    + "available is rejected with 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserved"),
            @ApiResponse(responseCode = "400", description = "Quantity out of range or order missing",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item with this identifier",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Fewer units available than were requested",
                    content = @Content)
    })
    InventoryItemResponse reserve(@Parameter(description = "Identifier of the item") UUID id,
                                  ReserveMaterialRequest request,
                                  Authentication authentication);

    @Operation(summary = "Give a reservation back",
            description = "For an estimate that was rejected, or one that expired. Stock on hand "
                    + "does not move: nothing had left.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Released"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item, or no open reservation "
                    + "for this order", content = @Content)
    })
    ResponseEntity<Void> release(@Parameter(description = "Identifier of the item") UUID id,
                                 @Parameter(description = "Service order holding the reservation") UUID serviceOrderId,
                                 Authentication authentication);

    @Operation(summary = "Take the reserved units off the shelf",
            description = "The baixa. Withdraws exactly what the order reserved — there is no way "
                    + "to withdraw material no approved estimate authorized, which is the rule the "
                    + "whole slice exists to keep. An order with no open reservation gets 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawn"),
            @ApiResponse(responseCode = "400", description = "Order missing from the body", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "404", description = "No active item, or no open reservation "
                    + "for this order", content = @Content)
    })
    StockWithdrawalResponse withdraw(@Parameter(description = "Identifier of the item") UUID id,
                                     WithdrawMaterialRequest request,
                                     Authentication authentication);
}
