package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.List;

import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.MaterialLineItem;
import com.jacafi.tech.serviceorder.domain.entity.RecordedEstimateDecision;
import com.jacafi.tech.serviceorder.domain.entity.ServiceLineItem;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.serviceorder.domain.entity.StatusHistory;

final class ServiceOrderPersistenceMapper {
    private ServiceOrderPersistenceMapper() {}

    static ServiceOrderJpaEntity toJpa(ServiceOrder order) {
        return new ServiceOrderJpaEntity(
                order.id(), order.customerId(), order.vehicleId(), order.status(), order.reportedIssue());
    }

    static ServiceOrderServiceLineJpaEntity toJpa(ServiceOrder order, ServiceLineItem line) {
        return new ServiceOrderServiceLineJpaEntity(
                line.id(),
                order.id(),
                line.serviceCatalogItemId(),
                line.serviceNameSnapshot(),
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

    static ServiceOrderEstimateDecisionJpaEntity toJpa(ServiceOrder order, RecordedEstimateDecision decision) {
        return new ServiceOrderEstimateDecisionJpaEntity(
                order.id(),
                decision.estimateId(),
                decision.decision(),
                decision.idempotencyKey(),
                decision.decidedAt());
    }

    static ServiceOrderStatusHistoryJpaEntity toJpa(ServiceOrder order, StatusHistory history) {
        return new ServiceOrderStatusHistoryJpaEntity(
                order.id(), history.previousStatus(), history.status(), history.actor(), history.occurredAt());
    }

    static ServiceOrder toDomain(
            ServiceOrderJpaEntity order,
            List<ServiceOrderServiceLineJpaEntity> serviceLines,
            List<ServiceOrderMaterialLineJpaEntity> materialLines,
            List<ServiceOrderEstimateJpaEntity> estimates,
            List<ServiceOrderStatusHistoryJpaEntity> statusHistory,
            List<ServiceOrderEstimateDecisionJpaEntity> decisions) {
        return ServiceOrder.restore(
                order.id(),
                order.customerId(),
                order.vehicleId(),
                order.reportedIssue(),
                order.status(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                serviceLines.stream()
                        .map(line -> ServiceLineItem.of(
                                line.id(),
                                line.serviceCatalogItemId(),
                                line.serviceNameSnapshot(),
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
                        .map(history -> new StatusHistory(
                                history.previousStatus(), history.status(), history.actor(), history.occurredAt()))
                        .toList(),
                decisions.stream()
                        .map(decision -> new RecordedEstimateDecision(
                                decision.idempotencyKey(),
                                decision.estimateId(),
                                decision.decision(),
                                decision.occurredAt()))
                        .toList());
    }
}
