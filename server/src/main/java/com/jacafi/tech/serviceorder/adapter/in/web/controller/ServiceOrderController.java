package com.jacafi.tech.serviceorder.adapter.in.web.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jacafi.tech.serviceorder.adapter.in.web.api.ServiceOrderApi;
import com.jacafi.tech.serviceorder.adapter.in.web.dto.*;
import com.jacafi.tech.serviceorder.application.service.*;
import com.jacafi.tech.shared.adapter.in.web.PageParameters;
import com.jacafi.tech.shared.adapter.in.web.SortableFields;
import com.jacafi.tech.shared.application.PageResult;

@RestController
@RequestMapping("/api/v1/service-orders")
public class ServiceOrderController implements ServiceOrderApi {
    private static final SortableFields QUEUE_SORT = SortableFields.of("id");

    private final OpenServiceOrderService open;
    private final FindServiceOrderStatusService findStatus;
    private final ApproveEstimateService approveEstimate;
    private final RejectEstimateService rejectEstimate;
    private final ListOperationalServiceOrdersService listOperational;
    private final UpdateServiceOrderStatusService updateStatus;

    public ServiceOrderController(
            OpenServiceOrderService open,
            FindServiceOrderStatusService findStatus,
            ApproveEstimateService approveEstimate,
            RejectEstimateService rejectEstimate,
            ListOperationalServiceOrdersService listOperational,
            UpdateServiceOrderStatusService updateStatus) {
        this.open = open;
        this.findStatus = findStatus;
        this.approveEstimate = approveEstimate;
        this.rejectEstimate = rejectEstimate;
        this.listOperational = listOperational;
        this.updateStatus = updateStatus;
    }

    @Override
    @PostMapping
    public ResponseEntity<ServiceOrderOpenedResponse> open(@Valid @RequestBody OpenServiceOrderRequest request) {
        var order = open.open(request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/service-orders/" + order.id()))
                .body(new ServiceOrderOpenedResponse(
                        order.id(), order.estimates().getFirst().id()));
    }

    @Override
    @GetMapping("/{serviceOrderId}/status")
    public ServiceOrderStatusResponse status(@PathVariable UUID serviceOrderId) {
        return ServiceOrderStatusResponse.from(findStatus.find(serviceOrderId));
    }

    @Override
    @PostMapping("/{serviceOrderId}/estimates/{estimateId}/approval")
    public EstimateResponse approveEstimate(
            @PathVariable UUID serviceOrderId,
            @PathVariable UUID estimateId,
            @Valid @RequestBody ApproveEstimateRequest request) {
        return EstimateResponse.from(approveEstimate.approve(serviceOrderId, estimateId, request.idempotencyKey()));
    }

    @Override
    @PostMapping("/{serviceOrderId}/estimates/{estimateId}/rejection")
    public EstimateResponse rejectEstimate(
            @PathVariable UUID serviceOrderId,
            @PathVariable UUID estimateId,
            @Valid @RequestBody RejectEstimateRequest request) {
        return EstimateResponse.from(rejectEstimate.reject(serviceOrderId, estimateId, request.idempotencyKey()));
    }

    @Override
    @PatchMapping("/{serviceOrderId}/status")
    public ServiceOrderStatusResponse updateStatus(
            @PathVariable UUID serviceOrderId, @Valid @RequestBody UpdateServiceOrderStatusRequest request) {
        return ServiceOrderStatusResponse.from(updateStatus.update(serviceOrderId, request.status()));
    }

    @Override
    @GetMapping
    public PageResult<ServiceOrderQueueItemResponse> list(PageParameters paging) {
        return listOperational.list(paging.toQuery(QUEUE_SORT)).map(ServiceOrderQueueItemResponse::from);
    }
}
