package com.jacafi.tech.serviceorder.adapter.in.web.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.jacafi.tech.serviceorder.application.service.OpenServiceOrderCommand;

public record OpenServiceOrderRequest(
        @NotNull UUID customerId,
        @NotNull UUID vehicleId,
        @NotBlank @Size(max = 2000) String reportedIssue,
        @NotEmpty List<@Valid RequestedServiceRequest> services,
        @NotNull List<@Valid RequestedMaterialRequest> materials) {
    public OpenServiceOrderCommand toCommand() {
        return new OpenServiceOrderCommand(
                customerId,
                vehicleId,
                reportedIssue,
                services.stream()
                        .map(item -> new OpenServiceOrderCommand.RequestedService(
                                item.serviceCatalogItemId(), item.quantity()))
                        .toList(),
                materials.stream()
                        .map(item ->
                                new OpenServiceOrderCommand.RequestedMaterial(item.inventoryItemId(), item.quantity()))
                        .toList());
    }
}
