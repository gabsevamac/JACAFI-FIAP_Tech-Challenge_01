package com.jacafi.tech.vehicle.api.dto;

import com.jacafi.tech.vehicle.application.VehiclePage;

import java.util.List;

/** One page of vehicles as the API exposes it. */
public record VehiclePageResponse(List<VehicleResponse> content,
                                  int page,
                                  int size,
                                  long totalElements,
                                  int totalPages) {

    public static VehiclePageResponse from(VehiclePage page) {
        return new VehiclePageResponse(page.content().stream().map(VehicleResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
