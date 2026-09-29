package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;

import com.jacafi.tech.serviceorder.domain.entity.Approval;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.LaborLineItem;
import com.jacafi.tech.serviceorder.domain.entity.MaterialLineItem;
import com.jacafi.tech.serviceorder.domain.entity.Rejection;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.StatusChange;

final class ServiceOrderPersistenceMapper {
    private ServiceOrderPersistenceMapper() {}

    static ServiceOrderJpaEntity toJpa(ServiceOrder order) {
        return new ServiceOrderJpaEntity(
                order.id(), order.customerId(), order.vehicleId(), order.status(), order.reportedIssue());
    }

    static ServiceOrderLaborLineJpaEntity toJpa(ServiceOrder order, LaborLineItem line) {
        return new ServiceOrderLaborLineJpaEntity(
                line.id(),
                order.id(),
                line.laborOperationId(),
                line.laborOperationNameSnapshot(),
                line.unitPriceSnapshot(),
                line.quantity());
    }

    static ServiceOrderMaterialLineJpaEntity toJpa(ServiceOrder order, MaterialLineItem line) {
        return new ServiceOrderMaterialLineJpaEntity(
                line.id(),
                order.id(),
                line.inventoryItemId(),
                line.materialNameSnapshot(),
                line.unitPriceSnapshot(),
                line.quantity());
    }

    static ServiceOrderEstimateJpaEntity toJpa(ServiceOrder order, Estimate estimate) {
        return new ServiceOrderEstimateJpaEntity(
                estimate.id(), order.id(), estimate.status(), estimate.totalAmount(), estimate.respondedAt());
    }

    static ServiceOrderEstimateApprovalJpaEntity toJpa(ServiceOrder order, Approval approval) {
        return new ServiceOrderEstimateApprovalJpaEntity(
                order.id(), approval.estimateId(), approval.idempotencyKey(), approval.approvedAt());
    }

    static ServiceOrderEstimateRejectionJpaEntity toJpa(ServiceOrder order, Rejection rejection) {
        return new ServiceOrderEstimateRejectionJpaEntity(
                order.id(), rejection.estimateId(), rejection.idempotencyKey(), rejection.rejectedAt());
    }

    static ServiceOrderStatusChangeJpaEntity toJpa(ServiceOrder order, StatusChange history) {
        return new ServiceOrderStatusChangeJpaEntity(
                order.id(), history.previousStatus(), history.status(), history.actor(), history.occurredAt());
    }

    static ServiceOrder toDomain(
            ServiceOrderJpaEntity order,
            List<ServiceOrderLaborLineJpaEntity> laborLines,
            List<ServiceOrderMaterialLineJpaEntity> materialLines,
            List<ServiceOrderEstimateJpaEntity> estimates,
            List<ServiceOrderStatusChangeJpaEntity> statusHistory,
            List<ServiceOrderEstimateApprovalJpaEntity> approvals,
            List<ServiceOrderEstimateRejectionJpaEntity> rejections) {
        return ServiceOrder.restore(
                order.id(),
                order.customerId(),
                order.vehicleId(),
                order.reportedIssue(),
                order.status(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                laborLines.stream()
                        .map(line -> LaborLineItem.of(
                                line.id(),
                                line.laborOperationId(),
                                line.laborOperationNameSnapshot(),
                                line.unitPriceSnapshot(),
                                line.quantity()))
                        .toList(),
                materialLines.stream()
                        .map(line -> MaterialLineItem.of(
                                line.id(),
                                line.inventoryItemId(),
                                line.materialNameSnapshot(),
                                line.unitPriceSnapshot(),
                                line.quantity()))
                        .toList(),
                estimates.stream()
                        .map(estimate -> Estimate.restore(
                                estimate.id(),
                                estimate.totalAmount(),
                                estimate.status(),
                                estimate.getCreatedAt(),
                                estimate.respondedAt()))
                        .toList(),
                statusHistory.stream()
                        .map(history -> new StatusChange(
                                history.previousStatus(), history.status(), history.actor(), history.occurredAt()))
                        .toList(),
                approvals.stream()
                        .map(approval ->
                                new Approval(approval.idempotencyKey(), approval.estimateId(), approval.approvedAt()))
                        .toList(),
                rejections.stream()
                        .map(rejection -> new Rejection(
                                rejection.idempotencyKey(), rejection.estimateId(), rejection.rejectedAt()))
                        .toList());
    }
}
