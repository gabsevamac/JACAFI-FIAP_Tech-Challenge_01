package com.jacafi.tech.vehicle.api.dto;

import java.util.List;

import com.jacafi.tech.vehicle.application.VehiclePage;

/** One page of vehicles as the API exposes it. */
public record VehiclePageResponse(
        List<VehicleResponse> content, int page, int size, long totalElements, int totalPages) {

    public static VehiclePageResponse from(VehiclePage page) {
        return new VehiclePageResponse(
                page.content().stream().map(VehicleResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
