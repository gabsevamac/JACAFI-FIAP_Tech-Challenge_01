package com.jacafi.tech.serviceorder.adapter.out.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.domain.entity.Estimate;
import com.jacafi.tech.serviceorder.domain.entity.ServiceOrder;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

@Component
public class ServiceOrderPersistenceAdapter implements ServiceOrderRepositoryPort {
    private final ServiceOrderJpaRepository orders;
    private final ServiceOrderServiceLineJpaRepository serviceLines;
    private final ServiceOrderMaterialLineJpaRepository materialLines;
    private final ServiceOrderEstimateJpaRepository estimates;
    private final ServiceOrderStatusHistoryJpaRepository statusHistory;
    private final ServiceOrderEstimateDecisionJpaRepository decisions;

    public ServiceOrderPersistenceAdapter(
            ServiceOrderJpaRepository orders,
            ServiceOrderServiceLineJpaRepository serviceLines,
            ServiceOrderMaterialLineJpaRepository materialLines,
            ServiceOrderEstimateJpaRepository estimates,
            ServiceOrderStatusHistoryJpaRepository statusHistory,
            ServiceOrderEstimateDecisionJpaRepository decisions) {
        this.orders = orders;
        this.serviceLines = serviceLines;
        this.materialLines = materialLines;
        this.estimates = estimates;
        this.statusHistory = statusHistory;
        this.decisions = decisions;
    }

    @Override
    public ServiceOrder save(ServiceOrder order) {
        ServiceOrderJpaEntity entity = orders.findById(order.id())
                .map(stored -> update(stored, order))
                .orElseGet(() -> ServiceOrderPersistenceMapper.toJpa(order));
        ServiceOrderJpaEntity saved = orders.saveAndFlush(entity);
        synchronizeServiceLines(order);
        synchronizeMaterialLines(order);
        synchronizeEstimates(order);
        synchronizeStatusHistory(order);
        synchronizeDecisions(order);
        return toDomain(saved);
    }

    @Override
    public Optional<ServiceOrder> findById(UUID id) {
        return orders.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public PageResult<ServiceOrder> findOperationalQueue(PageQuery query) {
        Page<ServiceOrderJpaEntity> page = orders.findOperationalQueue(PageRequest.of(query.page(), query.size()));
        return PageResult.of(
                page.getContent().stream().map(this::toDomain).toList(),
                query.page(),
                query.size(),
                page.getTotalElements());
    }

    private static ServiceOrderJpaEntity update(ServiceOrderJpaEntity stored, ServiceOrder order) {
        if (stored.getVersion() != order.version()) {
            throw new OptimisticLockingFailureException("Service order changed concurrently");
        }
        stored.apply(order);
        return stored;
    }

    private void synchronizeServiceLines(ServiceOrder order) {
        Map<UUID, ServiceOrderServiceLineJpaEntity> stored = indexed(
                serviceLines.findByServiceOrderIdAndDeletedAtIsNull(order.id()), ServiceOrderServiceLineJpaEntity::id);
        List<ServiceOrderServiceLineJpaEntity> created = order.serviceLines().stream()
                .filter(line -> !stored.containsKey(line.id()))
                .map(line -> ServiceOrderPersistenceMapper.toJpa(order, line))
                .toList();
        requireAppendOnly(stored.size(), order.serviceLines().size());
        serviceLines.saveAll(created);
        serviceLines.flush();
    }

    private void synchronizeMaterialLines(ServiceOrder order) {
        Map<UUID, ServiceOrderMaterialLineJpaEntity> stored = indexed(
                materialLines.findByServiceOrderIdAndDeletedAtIsNull(order.id()),
                ServiceOrderMaterialLineJpaEntity::id);
        List<ServiceOrderMaterialLineJpaEntity> created = order.materialLines().stream()
                .filter(line -> !stored.containsKey(line.id()))
                .map(line -> ServiceOrderPersistenceMapper.toJpa(order, line))
                .toList();
        requireAppendOnly(stored.size(), order.materialLines().size());
        materialLines.saveAll(created);
        materialLines.flush();
    }

    private void synchronizeEstimates(ServiceOrder order) {
        Map<UUID, ServiceOrderEstimateJpaEntity> stored = indexed(
                estimates.findByServiceOrderIdAndDeletedAtIsNull(order.id()), ServiceOrderEstimateJpaEntity::id);
        List<ServiceOrderEstimateJpaEntity> created = order.estimates().stream()
                .filter(estimate -> !stored.containsKey(estimate.id()))
                .map(estimate -> ServiceOrderPersistenceMapper.toJpa(order, estimate))
                .toList();
        for (Estimate estimate : order.estimates()) {
            ServiceOrderEstimateJpaEntity existing = stored.get(estimate.id());
            if (existing != null) {
                existing.apply(estimate);
            }
        }
        requireAppendOnly(stored.size(), order.estimates().size());
        estimates.saveAll(created);
        estimates.flush();
    }

    private void synchronizeStatusHistory(ServiceOrder order) {
        List<ServiceOrderStatusHistoryJpaEntity> stored = statusHistory.findByServiceOrderIdOrderById(order.id());
        requireAppendOnly(stored.size(), order.statusHistory().size());
        statusHistory.saveAll(order
                .statusHistory()
                .subList(stored.size(), order.statusHistory().size())
                .stream()
                .map(history -> ServiceOrderPersistenceMapper.toJpa(order, history))
                .toList());
        statusHistory.flush();
    }

    private void synchronizeDecisions(ServiceOrder order) {
        List<ServiceOrderEstimateDecisionJpaEntity> stored = decisions.findByServiceOrderIdOrderById(order.id());
        requireAppendOnly(stored.size(), order.recordedDecisions().size());
        decisions.saveAll(order
                .recordedDecisions()
                .subList(stored.size(), order.recordedDecisions().size())
                .stream()
                .map(decision -> ServiceOrderPersistenceMapper.toJpa(order, decision))
                .toList());
        decisions.flush();
    }

    private ServiceOrder toDomain(ServiceOrderJpaEntity order) {
        return ServiceOrderPersistenceMapper.toDomain(
                order,
                serviceLines.findByServiceOrderIdAndDeletedAtIsNull(order.id()),
                materialLines.findByServiceOrderIdAndDeletedAtIsNull(order.id()),
                estimates.findByServiceOrderIdAndDeletedAtIsNull(order.id()),
                statusHistory.findByServiceOrderIdOrderById(order.id()),
                decisions.findByServiceOrderIdOrderById(order.id()));
    }

    private static <T> Map<UUID, T> indexed(List<T> values, java.util.function.Function<T, UUID> id) {
        Map<UUID, T> indexed = new HashMap<>();
        for (T value : values) {
            indexed.put(id.apply(value), value);
        }
        return indexed;
    }

    private static void requireAppendOnly(int stored, int current) {
        if (current < stored) {
            throw new IllegalStateException("Service order records cannot be removed");
        }
    }
}
