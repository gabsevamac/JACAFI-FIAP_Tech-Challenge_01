package com.jacafi.tech.vehicle.api;

import java.net.URI;
import java.util.UUID;

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

import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.web.PageParameters;
import com.jacafi.tech.shared.web.SortableFields;
import com.jacafi.tech.vehicle.api.dto.RegisterVehicleRequest;
import com.jacafi.tech.vehicle.api.dto.UpdateVehicleRequest;
import com.jacafi.tech.vehicle.api.dto.VehicleResponse;
import com.jacafi.tech.vehicle.application.FindVehicleUseCase;
import com.jacafi.tech.vehicle.application.ListCustomerVehiclesUseCase;
import com.jacafi.tech.vehicle.application.RegisterVehicleCommand;
import com.jacafi.tech.vehicle.application.RegisterVehicleUseCase;
import com.jacafi.tech.vehicle.application.RemoveVehicleUseCase;
import com.jacafi.tech.vehicle.application.UpdateVehicleCommand;
import com.jacafi.tech.vehicle.application.UpdateVehicleUseCase;
import com.jacafi.tech.vehicle.domain.Vehicle;

/**
 * REST surface of the vehicle slice. Every endpoint requires a JWT.
 *
 * <p>Thin on purpose: it turns HTTP into a command, delegates, and turns the result into a DTO.
 * The rules live in the aggregate, the policies in the use cases, the mapping of a failure onto a
 * status code in {@link VehicleExceptionHandler}, and the API description in {@link VehicleApi}.
 *
 * <p>The author of every write comes from the authenticated principal, never from the request
 * body: a client that could name its own actor would make the audit trail worthless.
 */
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController implements VehicleApi {

    /** Cap on page size, so a caller cannot ask for the whole table in one request. */
    /**
     * What a client may sort this collection by, plus the tie-breaker.
     *
     * <p>Short on purpose. Every name here is a promise the API keeps, and a field added because
     * it happened to exist on the entity is a promise nobody decided to make. The ceiling on page
     * size lives in {@code PageParameters}, shared by every collection.
     */
    private static final SortableFields SORTABLE =
            SortableFields.of("id", "registeredAt", "make", "model", "modelYear");

    private final RegisterVehicleUseCase registerVehicle;
    private final UpdateVehicleUseCase updateVehicle;
    private final RemoveVehicleUseCase removeVehicle;
    private final FindVehicleUseCase findVehicle;
    private final ListCustomerVehiclesUseCase listCustomerVehicles;

    public VehicleController(
            RegisterVehicleUseCase registerVehicle,
            UpdateVehicleUseCase updateVehicle,
            RemoveVehicleUseCase removeVehicle,
            FindVehicleUseCase findVehicle,
            ListCustomerVehiclesUseCase listCustomerVehicles) {
        this.registerVehicle = registerVehicle;
        this.updateVehicle = updateVehicle;
        this.removeVehicle = removeVehicle;
        this.findVehicle = findVehicle;
        this.listCustomerVehicles = listCustomerVehicles;
    }

    @Override
    @PostMapping
    public ResponseEntity<VehicleResponse> register(
            @Valid @RequestBody RegisterVehicleRequest request, Authentication authentication) {
        Vehicle vehicle = registerVehicle.register(new RegisterVehicleCommand(
                request.licensePlate(),
                request.make(),
                request.model(),
                request.modelYear(),
                request.customerId(),
                authentication.getName()));

        URI location = UriComponentsBuilder.fromPath("/api/v1/vehicles/{id}")
                .buildAndExpand(vehicle.getId())
                .toUri();
        return ResponseEntity.created(location).body(VehicleResponse.from(vehicle));
    }

    @Override
    @GetMapping("/{id}")
    public VehicleResponse findById(@PathVariable UUID id) {
        return VehicleResponse.from(findVehicle.byId(id));
    }

    /**
     * Two queries behind one collection URI, because they answer different questions: a plate
     * identifies exactly one vehicle, while a customer owns a list of them. Asking for both at
     * once, or for neither, is a client error rather than a guess on our part.
     */
    @Override
    @GetMapping
    public ResponseEntity<?> findByQuery(
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) UUID customerId,
            PageParameters paging) {
        if ((licensePlate == null) == (customerId == null)) {
            throw new IllegalArgumentException("Exactly one of licensePlate or customerId must be provided");
        }

        if (licensePlate != null) {
            return ResponseEntity.ok(VehicleResponse.from(findVehicle.byLicensePlate(licensePlate)));
        }

        PageResult<VehicleResponse> page =
                listCustomerVehicles.list(customerId, paging.toQuery(SORTABLE)).map(VehicleResponse::from);
        return ResponseEntity.ok(page);
    }

    @Override
    @PutMapping("/{id}")
    public VehicleResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateVehicleRequest request, Authentication authentication) {
        return VehicleResponse.from(updateVehicle.update(new UpdateVehicleCommand(
                id, request.make(), request.model(), request.modelYear(), authentication.getName())));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable UUID id, Authentication authentication) {
        removeVehicle.remove(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
