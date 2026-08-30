package com.jacafi.tech.inventory.domain.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.jacafi.tech.inventory.domain.exception.InsufficientStockException;
import com.jacafi.tech.inventory.domain.exception.ReservationNotFoundException;

public final class InventoryItem {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int MAX_NAME_LENGTH = 120;
    private final UUID id;
    private final MaterialType type;
    private final Map<UUID, Reservation> reservations;
    private final long version;
    private final Instant registeredAt;
    private String name;
    private BigDecimal unitPrice;
    private Stock stockOnHand;
    private Instant updatedAt;
    private Instant removedAt;

    private InventoryItem(
            UUID id,
            String name,
            MaterialType type,
            BigDecimal unitPrice,
            Stock stockOnHand,
            Collection<Reservation> reservations,
            long version,
            Instant registeredAt,
            Instant updatedAt,
            Instant removedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = requireName(name);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.unitPrice = requirePrice(unitPrice);
        this.stockOnHand = Objects.requireNonNull(stockOnHand, "stockOnHand must not be null");
        if (version < 0) throw new IllegalArgumentException("version must not be negative");
        this.version = version;
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.removedAt = removedAt;
        this.reservations = reservationsByOrder(reservations);
        if (stockOnHand.isLessThan(stockReserved()))
            throw new IllegalArgumentException("open reservations cannot exceed stock on hand");
    }

    public static InventoryItem register(
            UUID id, String name, MaterialType type, BigDecimal unitPrice, Stock initialStock, Clock clock) {
        Instant now = requireClock(clock).instant();
        return new InventoryItem(id, name, type, unitPrice, initialStock, List.of(), 0, now, now, null);
    }

    public static InventoryItem restore(
            UUID id,
            String name,
            MaterialType type,
            BigDecimal unitPrice,
            Stock stockOnHand,
            Collection<Reservation> reservations,
            long version,
            Instant registeredAt,
            Instant updatedAt,
            Instant removedAt) {
        return new InventoryItem(
                id, name, type, unitPrice, stockOnHand, reservations, version, registeredAt, updatedAt, removedAt);
    }

    public void update(String name, BigDecimal unitPrice, Clock clock) {
        requireActive();
        this.name = requireName(name);
        this.unitPrice = requirePrice(unitPrice);
        updatedAt = requireClock(clock).instant();
    }

    public void replenish(Stock quantity, Clock clock) {
        requireActive();
        requirePositive(quantity, "A replenishment must add at least one unit");
        stockOnHand = stockOnHand.plus(quantity);
        updatedAt = requireClock(clock).instant();
    }

    public Reservation reserve(UUID serviceOrderId, Stock quantity, Clock clock) {
        requireActive();
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        requirePositive(quantity, "A reservation must hold at least one unit");
        if (stockAvailable().isLessThan(quantity)) throw new InsufficientStockException(quantity, stockAvailable());
        Instant now = requireClock(clock).instant();
        Reservation reservation = reservations.compute(
                serviceOrderId,
                (ignored, current) -> current == null
                        ? Reservation.open(serviceOrderId, quantity, now)
                        : current.increasedBy(quantity));
        updatedAt = now;
        return reservation;
    }

    public Reservation releaseReservation(UUID serviceOrderId, Clock clock) {
        requireActive();
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Reservation released = reservations.remove(serviceOrderId);
        if (released == null) throw new ReservationNotFoundException(id, serviceOrderId);
        updatedAt = requireClock(clock).instant();
        return released;
    }

    public StockWithdrawal withdraw(UUID serviceOrderId, Clock clock) {
        Reservation reservation = releaseReservation(serviceOrderId, clock);
        stockOnHand = stockOnHand.minus(reservation.quantity());
        Instant now = requireClock(clock).instant();
        updatedAt = now;
        return new StockWithdrawal(id, serviceOrderId, reservation.quantity(), now);
    }

    public void remove(Clock clock) {
        requireActive();
        if (!reservations.isEmpty())
            throw new IllegalStateException("An inventory item with open reservations cannot be removed");
        removedAt = requireClock(clock).instant();
        updatedAt = removedAt;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public MaterialType type() {
        return type;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public Stock stockOnHand() {
        return stockOnHand;
    }

    public long version() {
        return version;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public boolean active() {
        return removedAt == null;
    }

    public Optional<Instant> removedAt() {
        return Optional.ofNullable(removedAt);
    }

    public List<Reservation> reservations() {
        return List.copyOf(reservations.values());
    }

    public Stock stockReserved() {
        return reservations.values().stream().map(Reservation::quantity).reduce(Stock.ZERO, Stock::plus);
    }

    public Stock stockAvailable() {
        return stockOnHand.minus(stockReserved());
    }

    private static Map<UUID, Reservation> reservationsByOrder(Collection<Reservation> values) {
        Objects.requireNonNull(values, "reservations must not be null");
        Map<UUID, Reservation> indexed = new LinkedHashMap<>();
        for (Reservation reservation : values) {
            if (indexed.put(reservation.serviceOrderId(), reservation) != null)
                throw new IllegalArgumentException("Only one open reservation is allowed per service order");
        }
        return indexed;
    }

    private void requireActive() {
        if (!active()) throw new IllegalStateException("A removed inventory item cannot be modified");
    }

    private static Clock requireClock(Clock clock) {
        return Objects.requireNonNull(clock, "clock must not be null");
    }

    private static void requirePositive(Stock value, String message) {
        if (value == null || !value.isPositive()) throw new IllegalArgumentException(message);
    }

    private static String requireName(String value) {
        if (value == null) throw new IllegalArgumentException("name must not be null");
        String normalized = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH)
            throw new IllegalArgumentException("name must contain between 1 and 120 characters");
        return normalized;
    }

    private static BigDecimal requirePrice(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("unitPrice must be a non-negative amount with at most two decimals");
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public String toString() {
        return "InventoryItem[id=%s, name=%s, type=%s, active=%s]".formatted(id, name, type, active());
    }
}
