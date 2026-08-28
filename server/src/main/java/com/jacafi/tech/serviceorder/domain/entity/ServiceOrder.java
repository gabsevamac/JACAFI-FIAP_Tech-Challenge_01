package com.jacafi.tech.serviceorder.domain.entity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Aggregate that enforces the approval gate before execution. */
public final class ServiceOrder {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final UUID id;
    private final UUID customerId;
    private final UUID vehicleId;
    private final Instant createdAt;
    private final long version;
    private final List<ServiceLineItem> serviceLines;
    private final List<MaterialLineItem> materialLines;
    private final List<Estimate> estimates;
    private final List<StatusHistory> statusHistory;
    private final List<RecordedEstimateDecision> recordedDecisions;
    private String reportedIssue;
    private ServiceOrderStatus status;
    private Instant updatedAt;

    private ServiceOrder(
            UUID id,
            UUID customerId,
            UUID vehicleId,
            String reportedIssue,
            ServiceOrderStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Collection<ServiceLineItem> serviceLines,
            Collection<MaterialLineItem> materialLines,
            Collection<Estimate> estimates,
            Collection<StatusHistory> statusHistory,
            Collection<RecordedEstimateDecision> recordedDecisions) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        this.reportedIssue = requireReportedIssue(reportedIssue);
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.serviceLines = new ArrayList<>(serviceLines);
        this.materialLines = new ArrayList<>(materialLines);
        this.estimates = new ArrayList<>(estimates);
        this.statusHistory = new ArrayList<>(statusHistory);
        this.recordedDecisions = new ArrayList<>(recordedDecisions);
    }

    public static ServiceOrder open(
            UUID id, UUID customerId, UUID vehicleId, String reportedIssue, String actor, Clock clock) {
        return open(id, customerId, vehicleId, reportedIssue, List.of(), List.of(), actor, clock);
    }

    public static ServiceOrder open(
            UUID id,
            UUID customerId,
            UUID vehicleId,
            String reportedIssue,
            Collection<ServiceLineItem> serviceLines,
            Collection<MaterialLineItem> materialLines,
            String actor,
            Clock clock) {
        Instant now = requireClock(clock).instant();
        return new ServiceOrder(
                id,
                customerId,
                vehicleId,
                reportedIssue,
                ServiceOrderStatus.RECEIVED,
                0,
                now,
                now,
                serviceLines,
                materialLines,
                List.of(),
                List.of(new StatusHistory(null, ServiceOrderStatus.RECEIVED, requireActor(actor), now)),
                List.of());
    }

    public static ServiceOrder restore(
            UUID id,
            UUID customerId,
            UUID vehicleId,
            String reportedIssue,
            ServiceOrderStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Collection<ServiceLineItem> serviceLines,
            Collection<MaterialLineItem> materialLines,
            Collection<Estimate> estimates,
            Collection<StatusHistory> statusHistory,
            Collection<RecordedEstimateDecision> recordedDecisions) {
        return new ServiceOrder(
                id,
                customerId,
                vehicleId,
                reportedIssue,
                status,
                version,
                createdAt,
                updatedAt,
                serviceLines,
                materialLines,
                estimates,
                statusHistory,
                recordedDecisions);
    }

    public void startDiagnosis(String actor, Clock clock) {
        transition(ServiceOrderStatus.RECEIVED, ServiceOrderStatus.UNDER_DIAGNOSIS, actor, clock);
    }

    public void addServiceLine(ServiceLineItem line) {
        requireDiagnosis();
        serviceLines.add(Objects.requireNonNull(line, "line must not be null"));
    }

    public void addMaterialLine(MaterialLineItem line) {
        requireDiagnosis();
        materialLines.add(Objects.requireNonNull(line, "line must not be null"));
    }

    public Estimate generateEstimate(String actor, Clock clock) {
        requireStatus(ServiceOrderStatus.UNDER_DIAGNOSIS);
        Instant now = requireClock(clock).instant();
        Estimate estimate = Estimate.pending(UUID.randomUUID(), totalAmount(), now);
        estimates.add(estimate);
        transitionTo(ServiceOrderStatus.AWAITING_APPROVAL, actor, now);
        return estimate;
    }

    public Estimate decideEstimate(
            UUID estimateId, EstimateDecision decision, String idempotencyKey, String actor, Clock clock) {
        Objects.requireNonNull(estimateId, "estimateId must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        String key = requireIdempotencyKey(idempotencyKey);
        for (RecordedEstimateDecision recorded : recordedDecisions) {
            if (!recorded.idempotencyKey().equals(key)) {
                continue;
            }
            if (recorded.estimateId().equals(estimateId) && recorded.decision() == decision) {
                return estimateById(estimateId);
            }
            throw new IllegalStateException("Idempotency key was already used for a different decision");
        }
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        Estimate estimate = estimateById(estimateId);
        Instant now = requireClock(clock).instant();
        estimate.decide(decision, now);
        recordedDecisions.add(new RecordedEstimateDecision(key, estimateId, decision, now));
        transitionTo(
                decision == EstimateDecision.APPROVE
                        ? ServiceOrderStatus.IN_PROGRESS
                        : ServiceOrderStatus.UNDER_DIAGNOSIS,
                actor,
                now);
        return estimate;
    }

    public void complete(String actor, Clock clock) {
        transition(ServiceOrderStatus.IN_PROGRESS, ServiceOrderStatus.COMPLETED, actor, clock);
    }

    public void deliver(String actor, Clock clock) {
        transition(ServiceOrderStatus.COMPLETED, ServiceOrderStatus.DELIVERED, actor, clock);
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID vehicleId() {
        return vehicleId;
    }

    public String reportedIssue() {
        return reportedIssue;
    }

    public ServiceOrderStatus status() {
        return status;
    }

    public long version() {
        return version;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<ServiceLineItem> serviceLines() {
        return List.copyOf(serviceLines);
    }

    public List<MaterialLineItem> materialLines() {
        return List.copyOf(materialLines);
    }

    public List<Estimate> estimates() {
        return List.copyOf(estimates);
    }

    public List<StatusHistory> statusHistory() {
        return List.copyOf(statusHistory);
    }

    public List<RecordedEstimateDecision> recordedDecisions() {
        return List.copyOf(recordedDecisions);
    }

    private BigDecimal totalAmount() {
        return serviceLines.stream()
                .map(ServiceLineItem::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(materialLines.stream()
                        .map(MaterialLineItem::totalAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Estimate estimateById(UUID estimateId) {
        return estimates.stream()
                .filter(estimate -> estimate.id().equals(estimateId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("estimateId does not belong to this service order"));
    }

    private void transition(ServiceOrderStatus expected, ServiceOrderStatus next, String actor, Clock clock) {
        requireStatus(expected);
        transitionTo(next, actor, requireClock(clock).instant());
    }

    private void transitionTo(ServiceOrderStatus next, String actor, Instant at) {
        statusHistory.add(new StatusHistory(status, next, requireActor(actor), at));
        status = next;
        updatedAt = at;
    }

    private void requireDiagnosis() {
        requireStatus(ServiceOrderStatus.UNDER_DIAGNOSIS);
    }

    private void requireStatus(ServiceOrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Illegal service order status transition");
        }
    }

    private static Clock requireClock(Clock clock) {
        return Objects.requireNonNull(clock, "clock must not be null");
    }

    private static String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("actor must not be blank");
        }
        return actor;
    }

    private static String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 120) {
            throw new IllegalArgumentException("idempotencyKey must contain between 1 and 120 characters");
        }
        return value;
    }

    private static String requireReportedIssue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("reportedIssue must not be blank");
        }
        String normalized = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("reportedIssue must not be blank");
        }
        return normalized;
    }
}
