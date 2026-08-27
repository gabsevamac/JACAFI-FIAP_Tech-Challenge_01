package com.jacafi.tech.service_order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root for the Service Order (OS - Ordem de Serviço).
 *
 * <p>Represents the set of maintenance work performed on a customer vehicle during a visit, from
 * initial reception to vehicle delivery.
 *
 * <h2>Domain Invariants</h2>
 * <ul>
 *   <li>The approval functions as a strict gate: no execution without registered customer consent.</li>
 *   <li>Every status transition is governed by explicit domain rules and recorded in the status history.</li>
 *   <li>The total is always derived from the sum of launched services with prices frozen at launch time.</li>
 *   <li>Terminal states ({@link ServiceOrderStatus#DELIVERED}, {@link ServiceOrderStatus#REJECTED}) cannot transition.</li>
 * </ul>
 *
 * <p>Pure domain model: no framework, no ORM, no HTTP dependencies.
 */
public class ServiceOrder {

    private static final int PRICE_SCALE = 2;

    private final UUID id;
    private final UUID customerId;
    private final UUID vehicleId;
    private ServiceOrderStatus status;
    private BigDecimal total;
    private final List<LaunchedService> launchedServices;
    private String diagnosis;
    private final List<StatusChange> statusHistory;
    private final Instant registeredAt;
    private Instant updatedAt;
    private Instant removedAt;

    private ServiceOrder(Builder builder) {
        this.id = builder.id;
        this.customerId = builder.customerId;
        this.vehicleId = builder.vehicleId;
        this.status = builder.status;
        this.total = builder.total;
        this.launchedServices = new ArrayList<>(builder.launchedServices);
        this.diagnosis = builder.diagnosis;
        this.statusHistory = new ArrayList<>(builder.statusHistory);
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.removedAt = builder.removedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory method to open a new Service Order.
     *
     * <p>Corresponds to the {@code OpenServiceOrder} command, starting in {@link ServiceOrderStatus#RECEIVED}.
     */
    public static ServiceOrder open(UUID customerId, UUID vehicleId, Clock clock) {
        return builder().customerId(customerId).vehicleId(vehicleId).register(clock);
    }

    /**
     * Factory method to open a new Service Order with an explicit identifier.
     */
    public static ServiceOrder open(UUID id, UUID customerId, UUID vehicleId, Clock clock) {
        Objects.requireNonNull(id, "id must not be null");
        return builder().id(id).customerId(customerId).vehicleId(vehicleId).register(clock);
    }

    /**
     * Starts vehicle diagnosis by the technician ({@code StartDiagnosis} command).
     *
     * <p>Transitions {@code RECEIVED -> UNDER_DIAGNOSIS}.
     */
    public void startDiagnosis(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.RECEIVED, "start diagnosis");
        changeStatus(ServiceOrderStatus.UNDER_DIAGNOSIS, clock);
    }

    /**
     * Records technician diagnosis findings ({@code RecordDiagnosis} command).
     */
    public void recordDiagnosis(String findings, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (status != ServiceOrderStatus.UNDER_DIAGNOSIS && status != ServiceOrderStatus.RECEIVED) {
            throw new IllegalStateException("Cannot record diagnosis when service order is in status " + status);
        }
        if (findings == null || findings.isBlank()) {
            throw new IllegalArgumentException("Diagnosis findings must not be blank");
        }
        this.diagnosis = findings.trim();
        this.updatedAt = clock.instant();
    }

    /**
     * Launches a catalog service on this service order with frozen price at base price.
     */
    public LaunchedService launchService(Service service, int quantity, Clock clock) {
        Objects.requireNonNull(service, "service must not be null");
        return launchService(service, service.getBasePrice(), quantity, clock);
    }

    /**
     * Launches a service on this service order with a specific price and quantity.
     */
    public LaunchedService launchService(Service service, BigDecimal priceAtSale, int quantity, Clock clock) {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireNotTerminal("launch services on");

        if (status == ServiceOrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot launch services on a completed service order");
        }

        for (var existing : launchedServices) {
            if (existing.getServiceId().equals(service.getId())) {
                existing.updateQuantity(existing.getQuantity() + quantity);
                recalculateTotal();
                this.updatedAt = clock.instant();
                return existing;
            }
        }

        var item = LaunchedService.of(service.getId(), service.getDescription(), priceAtSale, quantity);
        this.launchedServices.add(item);
        recalculateTotal();
        this.updatedAt = clock.instant();
        return item;
    }

    /**
     * Removes a launched service from this order.
     */
    public boolean removeLaunchedService(UUID serviceId, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireNotTerminal("remove services from");
        if (status == ServiceOrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot remove services from a completed service order");
        }
        boolean removed = launchedServices.removeIf(item -> item.getServiceId().equals(serviceId));
        if (removed) {
            recalculateTotal();
            this.updatedAt = clock.instant();
        }
        return removed;
    }

    public boolean removeLaunchedService(UUID serviceId) {
        requireNotTerminal("remove services from");
        if (status == ServiceOrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot remove services from a completed service order");
        }
        boolean removed = launchedServices.removeIf(item -> item.getServiceId().equals(serviceId));
        if (removed) {
            recalculateTotal();
        }
        return removed;
    }

    /**
     * Calculates and sends estimate to the customer ({@code SendEstimate} command).
     *
     * <p>Transitions {@code UNDER_DIAGNOSIS -> AWAITING_APPROVAL}.
     */
    public void calculateAndSendEstimate(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (status != ServiceOrderStatus.UNDER_DIAGNOSIS && status != ServiceOrderStatus.RECEIVED) {
            throw new IllegalStateException("Cannot calculate and send estimate when status is " + status);
        }
        if (launchedServices.isEmpty()) {
            throw new IllegalStateException("Cannot send estimate without at least one launched service");
        }
        recalculateTotal();
        changeStatus(ServiceOrderStatus.AWAITING_APPROVAL, clock);
    }

    /**
     * Customer approves the estimate ({@code ApproveEstimate} command).
     *
     * <p>Transitions {@code AWAITING_APPROVAL -> IN_PROGRESS}.
     * This is the core domain gate: work cannot proceed without recorded customer approval.
     */
    public void approveEstimate(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL, "approve estimate");
        changeStatus(ServiceOrderStatus.IN_PROGRESS, clock);
    }

    /**
     * Customer rejects the estimate ({@code RejectEstimate} command).
     *
     * <p>Transitions {@code AWAITING_APPROVAL -> REJECTED}.
     */
    public void rejectEstimate(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL, "reject estimate");
        changeStatus(ServiceOrderStatus.REJECTED, clock);
    }

    /**
     * Estimate approval deadline expires ({@code ExpireEstimate} command).
     *
     * <p>Transitions {@code AWAITING_APPROVAL -> REJECTED}.
     */
    public void expireEstimate(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.AWAITING_APPROVAL, "expire estimate");
        changeStatus(ServiceOrderStatus.REJECTED, clock);
    }

    /**
     * Additional repair found during work execution ({@code AddAdditionalRepair} command).
     *
     * <p>Transitions {@code IN_PROGRESS -> AWAITING_APPROVAL}. This is the only backward loop in the
     * workflow, requiring customer approval for the supplementary estimate.
     */
    public void addAdditionalRepair(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.IN_PROGRESS, "add additional repair");
        changeStatus(ServiceOrderStatus.AWAITING_APPROVAL, clock);
    }

    /**
     * Technician completes all services on the vehicle ({@code CompleteServices} command).
     *
     * <p>Transitions {@code IN_PROGRESS -> COMPLETED}.
     */
    public void completeServices(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.IN_PROGRESS, "complete services");
        changeStatus(ServiceOrderStatus.COMPLETED, clock);
    }

    /**
     * Workshop delivers the vehicle back to customer ({@code DeliverVehicle} command).
     *
     * <p>Transitions {@code COMPLETED -> DELIVERED}.
     */
    public void deliverVehicle(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireStatus(ServiceOrderStatus.COMPLETED, "deliver vehicle");
        changeStatus(ServiceOrderStatus.DELIVERED, clock);
    }

    public void remove(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        this.removedAt = clock.instant();
        this.updatedAt = this.removedAt;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public void recalculateTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        for (var item : launchedServices) {
            sum = sum.add(item.getSubtotal());
        }
        this.total = sum.setScale(PRICE_SCALE, RoundingMode.HALF_EVEN);
    }

    private void changeStatus(ServiceOrderStatus targetStatus, Clock clock) {
        var previous = this.status;
        this.status = targetStatus;
        this.updatedAt = clock.instant();
        recordStatusTransition(previous, targetStatus, clock);
    }

    private void recordStatusTransition(ServiceOrderStatus from, ServiceOrderStatus to, Clock clock) {
        statusHistory.add(
                from == null
                        ? StatusChange.initial(to, clock.instant(), "system")
                        : StatusChange.transition(from, to, clock.instant(), "system"));
    }

    private void requireStatus(ServiceOrderStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalStateException("Cannot " + action + " when service order is in status " + status
                    + " (expected: " + expected + ")");
        }
    }

    private void requireNotTerminal(String action) {
        if (status != null && status.isTerminal()) {
            throw new IllegalStateException("Cannot " + action + " a service order in terminal status " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public ServiceOrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total != null ? total : BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public List<LaunchedService> getLaunchedServices() {
        return Collections.unmodifiableList(launchedServices);
    }

    public Optional<String> getDiagnosis() {
        return Optional.ofNullable(diagnosis);
    }

    public List<StatusChange> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Optional<Instant> getRemovedAt() {
        return Optional.ofNullable(removedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceOrder that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static final class Builder {
        private UUID id;
        private UUID customerId;
        private UUID vehicleId;
        private ServiceOrderStatus status;
        private BigDecimal total;
        private List<LaunchedService> launchedServices = new ArrayList<>();
        private String diagnosis;
        private List<StatusChange> statusHistory = new ArrayList<>();
        private Instant registeredAt;
        private Instant updatedAt;
        private Instant removedAt;

        private Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder customerId(UUID customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder vehicleId(UUID vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public Builder status(ServiceOrderStatus status) {
            this.status = status;
            return this;
        }

        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder launchedServices(List<LaunchedService> launchedServices) {
            if (launchedServices != null) {
                this.launchedServices = new ArrayList<>(launchedServices);
            }
            return this;
        }

        public Builder diagnosis(String diagnosis) {
            this.diagnosis = diagnosis;
            return this;
        }

        public Builder statusHistory(List<StatusChange> statusHistory) {
            if (statusHistory != null) {
                this.statusHistory = new ArrayList<>(statusHistory);
            }
            return this;
        }

        public Builder registeredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder removedAt(Instant removedAt) {
            this.removedAt = removedAt;
            return this;
        }

        public ServiceOrder register(Clock clock) {
            Objects.requireNonNull(clock, "clock must not be null");
            if (this.id == null) {
                this.id = UUID.randomUUID();
            }
            this.customerId = Objects.requireNonNull(this.customerId, "customerId must not be null");
            this.vehicleId = Objects.requireNonNull(this.vehicleId, "vehicleId must not be null");
            this.status = ServiceOrderStatus.RECEIVED;
            this.total = BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
            this.registeredAt = clock.instant();
            this.updatedAt = this.registeredAt;
            this.statusHistory.add(StatusChange.initial(ServiceOrderStatus.RECEIVED, this.registeredAt, "system"));
            return new ServiceOrder(this);
        }

        public ServiceOrder restore() {
            Objects.requireNonNull(this.id, "id must not be null");
            Objects.requireNonNull(this.customerId, "customerId must not be null");
            Objects.requireNonNull(this.vehicleId, "vehicleId must not be null");
            Objects.requireNonNull(this.status, "status must not be null");
            if (this.total == null) {
                this.total = BigDecimal.ZERO.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
            }
            return new ServiceOrder(this);
        }
    }
}
