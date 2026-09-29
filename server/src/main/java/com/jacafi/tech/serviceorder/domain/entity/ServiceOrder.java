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

public final class ServiceOrder {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final UUID id;
    private final UUID customerId;
    private final UUID vehicleId;
    private final Instant createdAt;
    private final long version;
    private final List<LaborLineItem> laborLines;
    private final List<MaterialLineItem> materialLines;
    private final List<Estimate> estimates;
    private final List<StatusChange> statusHistory;
    private final List<Approval> approvals;
    private final List<Rejection> rejections;
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
            Collection<LaborLineItem> laborLines,
            Collection<MaterialLineItem> materialLines,
            Collection<Estimate> estimates,
            Collection<StatusChange> statusHistory,
            Collection<Approval> approvals,
            Collection<Rejection> rejections) {
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
        this.laborLines = new ArrayList<>(laborLines);
        this.materialLines = new ArrayList<>(materialLines);
        this.estimates = new ArrayList<>(estimates);
        this.statusHistory = new ArrayList<>(statusHistory);
        this.approvals = new ArrayList<>(approvals);
        this.rejections = new ArrayList<>(rejections);
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
            Collection<LaborLineItem> laborLines,
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
                laborLines,
                materialLines,
                List.of(),
                List.of(new StatusChange(null, ServiceOrderStatus.RECEIVED, requireActor(actor), now)),
                List.of(),
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
            Collection<LaborLineItem> laborLines,
            Collection<MaterialLineItem> materialLines,
            Collection<Estimate> estimates,
            Collection<StatusChange> statusHistory,
            Collection<Approval> approvals,
            Collection<Rejection> rejections) {
        return new ServiceOrder(
                id,
                customerId,
                vehicleId,
                reportedIssue,
                status,
                version,
                createdAt,
                updatedAt,
                laborLines,
                materialLines,
                estimates,
                statusHistory,
                approvals,
                rejections);
    }

    public void startDiagnosis(String actor, Clock clock) {
        transition(ServiceOrderStatus.RECEIVED, ServiceOrderStatus.UNDER_DIAGNOSIS, actor, clock);
    }

    public void addLaborLine(LaborLineItem line) {
        requireDiagnosis();
        laborLines.add(Objects.requireNonNull(line, "line must not be null"));
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

    public Estimate approveEstimate(UUID estimateId, String idempotencyKey, String actor, Clock clock) {
        Objects.requireNonNull(estimateId, "estimateId must not be null");
        String key = requireIdempotencyKey(idempotencyKey);
        if (approvals.stream()
                .anyMatch(approval -> approval.idempotencyKey().equals(key)
                        && approval.estimateId().equals(estimateId))) {
            return estimateById(estimateId);
        }
        requireUnusedIdempotencyKey(key);
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        Estimate estimate = estimateById(estimateId);
        Instant now = requireClock(clock).instant();
        estimate.approve(now);
        approvals.add(new Approval(key, estimateId, now));
        transitionTo(ServiceOrderStatus.IN_PROGRESS, actor, now);
        return estimate;
    }

    public Estimate rejectEstimate(UUID estimateId, String idempotencyKey, String actor, Clock clock) {
        Objects.requireNonNull(estimateId, "estimateId must not be null");
        String key = requireIdempotencyKey(idempotencyKey);
        if (rejections.stream()
                .anyMatch(rejection -> rejection.idempotencyKey().equals(key)
                        && rejection.estimateId().equals(estimateId))) {
            return estimateById(estimateId);
        }
        requireUnusedIdempotencyKey(key);
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        Estimate estimate = estimateById(estimateId);
        Instant now = requireClock(clock).instant();
        estimate.reject(now);
        rejections.add(new Rejection(key, estimateId, now));
        transitionTo(ServiceOrderStatus.UNDER_DIAGNOSIS, actor, now);
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

    public List<LaborLineItem> laborLines() {
        return List.copyOf(laborLines);
    }

    public List<MaterialLineItem> materialLines() {
        return List.copyOf(materialLines);
    }

    public List<Estimate> estimates() {
        return List.copyOf(estimates);
    }

    public List<StatusChange> statusHistory() {
        return List.copyOf(statusHistory);
    }

    public List<Approval> approvals() {
        return List.copyOf(approvals);
    }

    public List<Rejection> rejections() {
        return List.copyOf(rejections);
    }

    private BigDecimal totalAmount() {
        return laborLines.stream()
                .map(LaborLineItem::totalAmount)
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
        statusHistory.add(new StatusChange(status, next, requireActor(actor), at));
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

    private void requireUnusedIdempotencyKey(String key) {
        boolean used = approvals.stream()
                        .anyMatch(approval -> approval.idempotencyKey().equals(key))
                || rejections.stream()
                        .anyMatch(rejection -> rejection.idempotencyKey().equals(key));
        if (used) {
            throw new IllegalStateException("Idempotency key was already used for a different approval or rejection");
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
