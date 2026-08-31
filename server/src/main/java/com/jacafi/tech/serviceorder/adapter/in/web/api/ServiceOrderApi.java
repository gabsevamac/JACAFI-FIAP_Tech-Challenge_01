package com.jacafi.tech.serviceorder.adapter.in.web.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jacafi.tech.serviceorder.adapter.in.web.dto.EstimateDecisionRequest;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.EstimateResponse;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.OpenServiceOrderRequest;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.ServiceOrderOpenedResponse;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.ServiceOrderQueueItemResponse;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.ServiceOrderStatusResponse;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.UpdateServiceOrderStatusRequest;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.application.PageResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Service orders", description = "Opening, tracking and budget approval for workshop service orders")
@SecurityRequirement(name = "bearer-jwt")
public interface ServiceOrderApi {
    @Operation(summary = "Open a service order and generate its first estimate")
    ResponseEntity<ServiceOrderOpenedResponse> open(OpenServiceOrderRequest request);

    @Operation(summary = "Find the current service order status")
    ServiceOrderStatusResponse status(UUID serviceOrderId);

    @Operation(summary = "Approve or reject an estimate idempotently")
    EstimateResponse decide(UUID serviceOrderId, UUID estimateId, EstimateDecisionRequest request);

    @Operation(summary = "Update a service order status and request customer notification")
    ServiceOrderStatusResponse updateStatus(UUID serviceOrderId, UpdateServiceOrderStatusRequest request);

    @Operation(summary = "List operational service orders in priority order")
    PageResult<ServiceOrderQueueItemResponse> list(PageParameters paging);
}
