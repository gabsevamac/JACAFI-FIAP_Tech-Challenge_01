package com.jacafi.tech.inventory.domain;

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

/**
 * One part or supply, seen from the angle of how many of it the workshop has — the
 * {@code InventoryItem} of §9 of the dictionary, and the root of the {@code Inventory} aggregate.
 *
 * <h2>Why the root is the item and not the whole stock</h2>
 *
 * <p>The Event Storming board names the aggregate {@code Inventory} and lists {@code InventoryItem}
 * and {@code Reservation} inside it. Read as one object holding every material in the workshop,
 * that boundary would put every reservation, every replenishment and every withdrawal in the
 * building behind a single lock: two mechanics withdrawing two unrelated parts would serialize
 * against each other. Read as one item with its own reservations — which is what this class is —
 * the boundary encloses exactly the invariant that needs protecting, and nothing more.
 *
 * <p>That invariant is: <em>the units reserved for service orders never exceed the units on
 * hand</em>. It is per item, always. No rule in this domain spans two different materials, so no
 * consistency boundary needs to either.
 *
 * <h2>What it enforces</h2>
 *
 * <ul>
 *   <li>Stock never goes negative, and neither does anything derived from it.</li>
 *   <li>Nothing is reserved beyond what is available — on hand minus what other orders hold.</li>
 *   <li>Nothing is withdrawn without a reservation naming the service order that authorized it.
 *       This is the domain vision made mechanical: "nenhuma peça sai do estoque sem vínculo com
 *       uma ordem aprovada".</li>
 *   <li>A removed item takes no operation at all.</li>
 * </ul>
 *
 * <p>Q3 of §8 — at which moment the part leaves stock — is answered by the board rather than by
 * this class: reserve when the estimate is approved, withdraw when the services are completed,
 * release when the estimate is rejected. The three verbs below are that answer.
 *
 * <p>Pure domain: no framework, no ORM, no HTTP, and no personal data of any kind — a material and
 * a quantity identify nobody. Time arrives as a {@link Clock} parameter so that every rule
 * involving instants is deterministic under test.
 */
public class InventoryItem {

    /** Matches the {@code name} column; refusing here beats a truncation error from the driver. */
    private static final int MAX_NAME_LENGTH = 120;

    /** Money is counted in centavos: a price with more decimals than that is a mistake. */
    private static final int PRICE_SCALE = 2;

    /** Collapses the runs of whitespace a human leaves behind when typing a name. */
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private final UUID id;

    private String name;

    private final MaterialType type;

    private BigDecimal unitPrice;

    /** Everything physically on the shelf, including the units reserved for open orders. */
    private Stock stockOnHand;

    /**
     * Open reservations, keyed by the service order that holds them.
     *
     * <p>Keyed, and not a list, because one order holds at most one reservation per item: an
     * additional repair on an order that already reserved enlarges what it holds rather than
     * opening a second claim. Release and withdrawal address a reservation by order for the same
     * reason — that is the only name the calling policy knows.
     *
     * <p>Settled reservations leave the map. What happened to them is in the audit trail, which is
     * append-only and exists for exactly that; keeping them here would grow the aggregate without
     * bound for no invariant's sake.
     */
    private final Map<UUID, Reservation> reservations;

    private final Instant registeredAt;
    private Instant updatedAt;
    private Instant removedAt;

    private InventoryItem(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.type = builder.type;
        this.unitPrice = builder.unitPrice;
        this.stockOnHand = builder.stockOnHand;
        this.reservations = new LinkedHashMap<>(builder.reservations);
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.removedAt = builder.removedAt;
    }

    /**
     * Starts building an item. Finish with {@link Builder#register(Clock)} for a new one or
     * {@link Builder#restore()} to rebuild one that is already stored — the two mean different
     * things and enforce different rules.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Corrects the name and the unit price of the material.
     *
     * <p>{@link MaterialType} is absent from this signature on purpose: it is immutable after
     * registration, since reinterpreting a part as a supply would rewrite the meaning of every
     * withdrawal already made against the item.
     *
     * <p>The price is the one that will be charged from now on. It does not reach back into orders
     * already priced: a {@code MaterialLineItem} freezes the price at the moment it is launched,
     * which is what keeps a price rise today from altering an agreement closed yesterday.
     */
    public void update(String name, BigDecimal unitPrice, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        this.name = requireName(name);
        this.unitPrice = requireUnitPrice(unitPrice);
        this.updatedAt = clock.instant();
    }

    /**
     * Adds units to the shelf — the {@code ReplenishStock} command.
     *
     * <p>Reservations are untouched: replenishing raises what is on hand, so what is available
     * rises with it, and what other orders already hold stays theirs.
     */
    public void replenish(Stock quantity, Clock clock) {
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("A replenishment must add at least one unit");
        }

        this.stockOnHand = stockOnHand.plus(quantity);
        this.updatedAt = clock.instant();
    }

    /**
     * Holds units for a service order — the {@code ReserveMaterial} command, driven by the
     * {@code WhenEstimateApproved} policy.
     *
     * <p>Nothing leaves the shelf here. What changes is who may count on the units: they stop
     * being available to any other order. That is what makes an approved estimate mean something
     * before the mechanic actually reaches for the part.
     *
     * <p>Reserving twice for the same order enlarges the existing reservation instead of opening a
     * second one. That is the additional repair of the board: same order, more material, one
     * claim.
     *
     * @return the reservation as it now stands, enlarged if one was already open
     * @throws InsufficientStockException when fewer units are available than were asked for
     */
    public Reservation reserve(UUID serviceOrderId, Stock quantity, Clock clock) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        if (!quantity.isPositive()) {
            throw new IllegalArgumentException("A reservation must hold at least one unit");
        }
        if (stockAvailable().isLessThan(quantity)) {
            throw new InsufficientStockException(quantity, stockAvailable());
        }

        Reservation existing = reservations.get(serviceOrderId);
        Reservation reservation = existing == null
                ? Reservation.open(serviceOrderId, quantity, clock.instant())
                : existing.increasedBy(quantity);

        reservations.put(serviceOrderId, reservation);
        this.updatedAt = clock.instant();
        return reservation;
    }

    /**
     * Gives the units back to whoever needs them next — the {@code ReleaseReservation} command,
     * driven by {@code WhenEstimateRejected} and by {@code WhenApprovalDeadlineExpires}.
     *
     * <p>Stock on hand does not move: nothing had left. Only the claim disappears.
     *
     * <p>HS2 on the board is the reason the second policy exists at all — a reservation with no
     * deadline holds stock for an estimate nobody ever answers. This aggregate releases whenever
     * it is told to; deciding when that is belongs to the order's side of the policy.
     *
     * @return the reservation that was released, so the caller can record what it gave back
     * @throws ReservationNotFoundException when the order holds nothing here
     */
    public Reservation releaseReservation(UUID serviceOrderId, Clock clock) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        Reservation released = reservations.remove(serviceOrderId);
        if (released == null) {
            throw new ReservationNotFoundException(id, serviceOrderId);
        }

        this.updatedAt = clock.instant();
        return released;
    }

    /**
     * Takes the reserved units off the shelf — the {@code WithdrawMaterial} command, driven by
     * {@code WhenServicesCompleted}. This is the baixa.
     *
     * <p>It withdraws what the order reserved, and only that. There is no signature here for
     * withdrawing an arbitrary amount, and that absence <em>is</em> the invariant: material leaves
     * this shelf only against a reservation, and a reservation exists only because an estimate was
     * approved. A part cannot leave without an order behind it, because there is no method that
     * would let it.
     *
     * <p>Withdrawing less than was reserved — the mechanic needed three of the four bolts — is not
     * modelled. It would mean partially settling a reservation and deciding what happens to the
     * remainder, and the board has no answer for that yet. When it does, it arrives as its own
     * command, not as a quantity parameter here.
     *
     * @return the withdrawal, for the caller to record
     * @throws ReservationNotFoundException when the order holds nothing here, which means no
     *                                      approval ever authorized this material
     */
    public StockWithdrawal withdraw(UUID serviceOrderId, Clock clock) {
        Objects.requireNonNull(serviceOrderId, "serviceOrderId must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        Reservation reservation = reservations.remove(serviceOrderId);
        if (reservation == null) {
            throw new ReservationNotFoundException(id, serviceOrderId);
        }

        // Cannot go negative: a reservation was only ever accepted against available stock, and
        // available is what is on hand minus every other claim.
        this.stockOnHand = stockOnHand.minus(reservation.quantity());
        Instant now = clock.instant();
        this.updatedAt = now;

        return new StockWithdrawal(id, serviceOrderId, reservation.quantity(), now);
    }

    /**
     * Takes the material out of the catalogue — the {@code RemoveMaterial} command.
     *
     * <p>The row survives, like a removed vehicle's does, because withdrawals already recorded
     * point at it and a service history that dangles proves nothing. Nothing personal is erased
     * here: this slice holds no personal data, so removal is an ordinary catalogue decision rather
     * than an answer to Art. 18 VI.
     *
     * <p>An item with open reservations is refused. Removing it would strand material that some
     * approved order is counting on, and the workshop would find out at the lift.
     */
    public void remove(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        if (!reservations.isEmpty()) {
            throw new IllegalStateException("An inventory item with open reservations cannot be removed: " + id);
        }

        this.removedAt = clock.instant();
        this.updatedAt = this.removedAt;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MaterialType getType() {
        return type;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** Everything on the shelf, reserved units included. */
    public Stock getStockOnHand() {
        return stockOnHand;
    }

    /** What every open reservation holds, added up. */
    public Stock stockReserved() {
        return reservations.values().stream().map(Reservation::quantity).reduce(Stock.ZERO, Stock::plus);
    }

    /** What a new order could still be promised. Never negative, by the reservation rule. */
    public Stock stockAvailable() {
        return stockOnHand.minus(stockReserved());
    }

    /** Open reservations, in the order they were first opened. */
    public List<Reservation> getReservations() {
        return List.copyOf(reservations.values());
    }

    public Optional<Reservation> reservationFor(UUID serviceOrderId) {
        return Optional.ofNullable(reservations.get(serviceOrderId));
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Empty while the item is in the catalogue. */
    public Optional<Instant> getRemovedAt() {
        return Optional.ofNullable(removedAt);
    }

    private void requireActive() {
        if (isRemoved()) {
            throw new IllegalStateException("A removed inventory item cannot be modified: " + id);
        }
    }

    private static String requireName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        String normalized = WHITESPACE_RUN.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    /**
     * Rejects a price with more than two decimals instead of rounding it. Rounding money without
     * being asked is how a system starts disagreeing with the invoice it produced.
     */
    private static BigDecimal requireUnitPrice(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("unitPrice must not be null");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must be zero or positive");
        }
        if (value.stripTrailingZeros().scale() > PRICE_SCALE) {
            throw new IllegalArgumentException("unitPrice must have at most two decimal places");
        }
        return value.setScale(PRICE_SCALE, RoundingMode.UNNECESSARY);
    }

    /** Identity equality: two items are the same item when they share an identifier. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryItem other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Safe to log in full: a material name and a count identify no one. */
    @Override
    public String toString() {
        return "InventoryItem[id=%s, name=%s, type=%s, onHand=%s, reserved=%s, removed=%s]"
                .formatted(id, name, type, stockOnHand, stockReserved(), isRemoved());
    }

    /**
     * Builds an item by named step, the only way to obtain one.
     *
     * <p>Two terminal operations, because creating and rehydrating are different acts:
     * {@link #register(Clock)} applies every business rule and stamps the clock, while
     * {@link #restore()} rebuilds what storage already holds and applies none.
     *
     * <p>Named steps rather than positional arguments: this aggregate carries three quantities and
     * three timestamps that all look alike at a call site, and a swap between any two of them
     * would compile, pass review and produce a consistent but wrong record.
     */
    public static final class Builder {

        private UUID id;
        private String name;
        private MaterialType type;
        private BigDecimal unitPrice;
        private Stock stockOnHand = Stock.ZERO;
        private Map<UUID, Reservation> reservations = Map.of();
        private Instant registeredAt;
        private Instant updatedAt;
        private Instant removedAt;

        private Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(MaterialType type) {
            this.type = type;
            return this;
        }

        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        /** The opening balance on registration; whatever storage holds on rehydration. */
        public Builder stockOnHand(Stock stockOnHand) {
            this.stockOnHand = stockOnHand;
            return this;
        }

        /** Rehydration only: a new item has no reservations, by definition. */
        public Builder reservations(Collection<Reservation> reservations) {
            Map<UUID, Reservation> byOrder = new LinkedHashMap<>();
            for (Reservation reservation : reservations) {
                Reservation clash = byOrder.put(reservation.serviceOrderId(), reservation);
                if (clash != null) {
                    // Two open claims by one order on one item is a state this aggregate cannot
                    // produce. Finding it in storage means someone wrote around the aggregate.
                    throw new IllegalArgumentException(
                            "Two open reservations for service order " + reservation.serviceOrderId());
                }
            }
            this.reservations = byOrder;
            return this;
        }

        /** Rehydration only: a new registration takes its timestamps from the clock. */
        public Builder registeredAt(Instant registeredAt) {
            this.registeredAt = registeredAt;
            return this;
        }

        /** Rehydration only. */
        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /** Rehydration only, and only for an item that was removed. */
        public Builder removedAt(Instant removedAt) {
            this.removedAt = removedAt;
            return this;
        }

        /**
         * Registers a new material, applying every rule — the {@code RegisterMaterial} command.
         *
         * <p>Name uniqueness is not among them. An aggregate can only enforce invariants over its
         * own state, and uniqueness spans the whole catalogue; that check belongs to the
         * application layer, which owns the repository.
         *
         * @throws IllegalArgumentException when an attribute is missing or out of range
         */
        public InventoryItem register(Clock clock) {
            Objects.requireNonNull(clock, "clock must not be null");
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(stockOnHand, "stockOnHand must not be null");

            if (registeredAt != null || updatedAt != null || removedAt != null) {
                throw new IllegalArgumentException(
                        "Timestamps come from the clock when registering; they are for restore only");
            }
            if (!reservations.isEmpty()) {
                throw new IllegalArgumentException("A newly registered item holds no reservations");
            }

            this.name = requireName(name);
            this.unitPrice = requireUnitPrice(unitPrice);
            this.registeredAt = clock.instant();
            this.updatedAt = this.registeredAt;

            return new InventoryItem(this);
        }

        /**
         * Rebuilds an aggregate already stored, for the persistence layer only.
         *
         * <p>Business rules are not re-applied: this data was validated when it was first
         * accepted, and re-running the checks would let a rule introduced today reject a row
         * written yesterday.
         */
        public InventoryItem restore() {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(unitPrice, "unitPrice must not be null");
            Objects.requireNonNull(stockOnHand, "stockOnHand must not be null");
            Objects.requireNonNull(registeredAt, "registeredAt must not be null");
            Objects.requireNonNull(updatedAt, "updatedAt must not be null");

            return new InventoryItem(this);
        }
    }
}
