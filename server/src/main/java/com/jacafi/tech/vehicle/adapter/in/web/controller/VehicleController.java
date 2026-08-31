package com.jacafi.tech.vehicle.adapter.in.web.controller;

import java.net.URI;
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

import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.adapter.in.web.SortableFields;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.adapter.in.web.api.VehicleApi;
import com.jacafi.tech.vehicle.adapter.in.web.dto.RegisterVehicleRequest;
import com.jacafi.tech.vehicle.adapter.in.web.dto.UpdateVehicleRequest;
import com.jacafi.tech.vehicle.adapter.in.web.dto.VehicleResponse;
import com.jacafi.tech.vehicle.application.service.FindVehicleService;
import com.jacafi.tech.vehicle.application.service.ListCurrentCustomerVehiclesService;
import com.jacafi.tech.vehicle.application.service.ListCustomerVehiclesService;
import com.jacafi.tech.vehicle.application.service.RegisterVehicleService;
import com.jacafi.tech.vehicle.application.service.RemoveVehicleService;
import com.jacafi.tech.vehicle.application.service.UpdateVehicleService;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController implements VehicleApi {

    private static final SortableFields SORTABLE =
            SortableFields.of("id", "registeredAt", "make", "model", "modelYear");

    private final RegisterVehicleService registerVehicle;
    private final FindVehicleService findVehicle;
    private final ListCustomerVehiclesService listCustomerVehicles;
    private final ListCurrentCustomerVehiclesService listCurrentCustomerVehicles;
    private final UpdateVehicleService updateVehicle;
    private final RemoveVehicleService removeVehicle;

    public VehicleController(
            RegisterVehicleService registerVehicle,
            FindVehicleService findVehicle,
            ListCustomerVehiclesService listCustomerVehicles,
            ListCurrentCustomerVehiclesService listCurrentCustomerVehicles,
            UpdateVehicleService updateVehicle,
            RemoveVehicleService removeVehicle) {
        this.registerVehicle = registerVehicle;
        this.findVehicle = findVehicle;
        this.listCustomerVehicles = listCustomerVehicles;
        this.listCurrentCustomerVehicles = listCurrentCustomerVehicles;
        this.updateVehicle = updateVehicle;
        this.removeVehicle = removeVehicle;
    }

    @Override
    @PostMapping
    public ResponseEntity<VehicleResponse> register(@Valid @RequestBody RegisterVehicleRequest request) {
        var vehicle = registerVehicle.register(
                request.licensePlate(), request.make(), request.model(), request.modelYear(), request.customerId());
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + vehicle.id()))
                .body(VehicleResponse.from(vehicle));
    }

    @Override
    @GetMapping("/me")
    public PageResult<VehicleResponse> listMine(PageParameters paging) {
        return listCurrentCustomerVehicles.list(paging.toQuery(SORTABLE)).map(VehicleResponse::from);
    }

    @Override
    @GetMapping("/lookup")
    public VehicleResponse findByLicensePlate(@RequestParam String licensePlate) {
        return VehicleResponse.from(findVehicle.findByLicensePlate(licensePlate));
    }

    @Override
    @GetMapping
    public PageResult<VehicleResponse> list(@RequestParam UUID customerId, PageParameters paging) {
        return listCustomerVehicles.list(customerId, paging.toQuery(SORTABLE)).map(VehicleResponse::from);
    }

    @Override
    @GetMapping("/{vehicleId}")
    public VehicleResponse findById(@PathVariable UUID vehicleId) {
        return VehicleResponse.from(findVehicle.findById(vehicleId));
    }

    @Override
    @PutMapping("/{vehicleId}")
    public VehicleResponse update(@PathVariable UUID vehicleId, @Valid @RequestBody UpdateVehicleRequest request) {
        return VehicleResponse.from(
                updateVehicle.update(vehicleId, request.make(), request.model(), request.modelYear()));
    }

    @Override
    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> remove(@PathVariable UUID vehicleId) {
        removeVehicle.remove(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
