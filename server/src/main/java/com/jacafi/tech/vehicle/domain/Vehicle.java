package com.jacafi.tech.vehicle.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * A vehicle presented to the workshop for maintenance, described by make, model and model year.
 *
 * <p>Aggregate root. It carries a surrogate identity ({@code id}) for references and a business
 * identity in its {@link LicensePlate}: the workshop recognizes a returning vehicle by the plate,
 * which is why a known vehicle is linked to a new visit rather than registered again. The plate is
 * a natural search key, not a permanent identity — see the note on mutability in §9 of the
 * dictionary and HS8 on the Event Storming board.
 *
 * <p>Pure domain: no framework, no ORM, no HTTP. The only non-JDK import is the personal data
 * marker, which is an annotation and carries no behaviour. Time arrives as a {@link Clock}
 * parameter instead of being read from the ambient system, so that every rule involving dates is
 * deterministic under test.
 *
 * <p>No setters. State changes go through methods named after what the workshop actually does, and
 * each of them enforces the invariants that guard the change.
 *
 * <p>Construction goes through {@link #builder()}. Named steps rather than positional arguments,
 * because this aggregate has three pairs of interchangeable-looking values — the two identifiers,
 * make and model, and the three timestamps of a stored row — and a swap between any of them would
 * compile, pass review and produce a consistent but wrong record.
 */
public class Vehicle {

    private static final int EARLIEST_MODEL_YEAR = 1900;

    /** Collapses the runs of whitespace a human leaves behind when typing a name. */
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    private final UUID id;

    /**
     * Absent once the vehicle has been removed: the plate is erased from the aggregate, and the
     * persistence layer replaces the stored value with an irreversible token. Modelled as a
     * nullable field with an {@link Optional} accessor rather than a sealed hierarchy, because an
     * "anonymized plate" is not a concept the workshop has — the plate is simply gone.
     */
    @PersonalData("LGPD Art. 5 I — identifies a vehicle and, through it, its owner")
    private LicensePlate licensePlate;

    private String make;
    private String model;
    private int modelYear;

    /**
     * Reference to the customer responsible for the vehicle, by identifier.
     *
     * <p>Never an object reference: {@code Customer} is the root of another aggregate, owned by
     * another slice, and a slice does not import another slice's domain. Holding the identifier is
     * what keeps the two aggregates independently consistent, and what lets the customer slice be
     * built in parallel.
     */
    private final UUID customerId;

    private final Instant registeredAt;
    private Instant updatedAt;
    private Instant removedAt;

    private Vehicle(Builder builder) {
        this.id = builder.id;
        this.licensePlate = builder.licensePlate;
        this.make = builder.make;
        this.model = builder.model;
        this.modelYear = builder.modelYear;
        this.customerId = builder.customerId;
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.removedAt = builder.removedAt;
    }

    /**
     * Starts building a vehicle. Finish with {@link Builder#register(Clock)} for a new one or
     * {@link Builder#restore()} to rebuild one that is already stored — the two mean different
     * things and enforce different rules.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Corrects the descriptive attributes of the vehicle.
     *
     * <p>The license plate is absent from this signature on purpose: it is the vehicle's business
     * identity and is immutable after registration.
     *
     * <p>TODO: correcting a plate that was recorded wrongly is a use case of its own, with its own
     * audit entry and its own authorization — not a field on this method. Out of scope for the MVP,
     * and distinct from a plate that legitimately changed, which is HS8 and HS9 on the board.
     */
    public void update(String make, String model, int modelYear, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        this.make = requireText(make, "make");
        this.model = requireText(model, "model");
        this.modelYear = requireModelYearInRange(modelYear, clock);
        this.updatedAt = clock.instant();
    }

    /**
     * Removes the vehicle from the active registry, erasing its plate.
     *
     * <p>The record itself survives. The service history attached to it has to be kept, both as a
     * legal obligation and for warranty purposes (LGPD Art. 16 I), while the data subject retains
     * the right to have the personal data erased (Art. 18 VI). Dropping the plate satisfies the
     * second without breaking the first.
     *
     * <p>The aggregate only erases. Writing the irreversible token that takes the plate's place in
     * storage belongs to the persistence layer, which is where the unique index lives.
     */
    public void remove(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();

        this.licensePlate = null;
        this.removedAt = clock.instant();
        this.updatedAt = this.removedAt;
    }

    public boolean isRemoved() {
        return removedAt != null;
    }

    public UUID getId() {
        return id;
    }

    /** Empty once the vehicle has been removed. */
    public Optional<LicensePlate> getLicensePlate() {
        return Optional.ofNullable(licensePlate);
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getModelYear() {
        return modelYear;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Empty while the vehicle is active. */
    public Optional<Instant> getRemovedAt() {
        return Optional.ofNullable(removedAt);
    }

    private void requireActive() {
        if (isRemoved()) {
            throw new IllegalStateException("A removed vehicle cannot be modified: " + id);
        }
    }

    private static String requireText(String value, String attribute) {
        if (value == null) {
            throw new IllegalArgumentException(attribute + " must not be null");
        }
        String normalized = WHITESPACE_RUN.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(attribute + " must not be blank");
        }
        return normalized;
    }

    /**
     * Upper bound is next year, not this one: dealerships sell the coming model year before it
     * starts. The bound therefore moves with the clock, which is the reason the clock is a
     * parameter.
     */
    private static int requireModelYearInRange(int modelYear, Clock clock) {
        int latest = Year.now(clock).getValue() + 1;
        if (modelYear < EARLIEST_MODEL_YEAR || modelYear > latest) {
            throw new IllegalArgumentException(
                    "modelYear must be between %d and %d".formatted(EARLIEST_MODEL_YEAR, latest));
        }
        return modelYear;
    }

    /** Identity equality: two vehicles are the same vehicle when they share an identifier. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Never prints the full plate — masked while the vehicle is active, absent once removed.
     * Make, model and model year are left out as well: they add nothing to a log line and, taken
     * together with a masked plate, they narrow a vehicle down further than the mask intends.
     */
    @Override
    public String toString() {
        return "Vehicle[id=%s, licensePlate=%s, removed=%s]"
                .formatted(id, getLicensePlate().map(LicensePlate::masked).orElse("<erased>"), isRemoved());
    }

    /**
     * Builds a vehicle by named step, the only way to obtain one.
     *
     * <p>Two terminal operations, because creating and rehydrating are different acts:
     * {@link #register(Clock)} applies every business rule and stamps the clock, while
     * {@link #restore()} rebuilds what storage already holds and applies none.
     *
     * <p>There is deliberately no way to copy an existing vehicle with altered fields. Changes go
     * through {@link Vehicle#update} and {@link Vehicle#remove}, which enforce the invariants; a
     * copy step that accepted any field would be a way around the plate's immutability, which
     * {@code update} enforces precisely by not offering it.
     */
    public static final class Builder {

        private UUID id;
        private LicensePlate licensePlate;
        private String make;
        private String model;
        private Integer modelYear;
        private UUID customerId;
        private Instant registeredAt;
        private Instant updatedAt;
        private Instant removedAt;

        private Builder() {}

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder licensePlate(LicensePlate licensePlate) {
            this.licensePlate = licensePlate;
            return this;
        }

        public Builder make(String make) {
            this.make = make;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder modelYear(int modelYear) {
            this.modelYear = modelYear;
            return this;
        }

        public Builder customerId(UUID customerId) {
            this.customerId = customerId;
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

        /** Rehydration only, and only for a vehicle that was removed. */
        public Builder removedAt(Instant removedAt) {
            this.removedAt = removedAt;
            return this;
        }

        /**
         * Registers a new vehicle, applying every rule.
         *
         * <p>Plate uniqueness is not among them. An aggregate can only enforce invariants over its
         * own state, and uniqueness spans the whole collection — that check belongs to the
         * application layer, which owns the repository.
         *
         * @throws InvalidLicensePlateException from {@link LicensePlate} when the format is rejected
         * @throws IllegalArgumentException     when a required attribute is missing or out of range
         */
        public Vehicle register(Clock clock) {
            Objects.requireNonNull(clock, "clock must not be null");
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(licensePlate, "licensePlate must not be null");
            Objects.requireNonNull(customerId, "customerId must not be null");
            Objects.requireNonNull(modelYear, "modelYear must not be null");

            if (registeredAt != null || updatedAt != null || removedAt != null) {
                throw new IllegalArgumentException(
                        "Timestamps come from the clock when registering; they are for restore only");
            }

            this.make = requireText(make, "make");
            this.model = requireText(model, "model");
            this.modelYear = requireModelYearInRange(modelYear, clock);
            this.registeredAt = clock.instant();
            this.updatedAt = this.registeredAt;

            return new Vehicle(this);
        }

        /**
         * Rebuilds an aggregate already stored, for the persistence layer only.
         *
         * <p>Business rules are not re-applied: this data was validated when it was first accepted,
         * and re-running the checks would let a rule introduced today reject a row written
         * yesterday. A removed vehicle arrives here with no plate and a {@code removedAt}, which is
         * a legitimate state to rebuild and an impossible one to register.
         */
        public Vehicle restore() {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(customerId, "customerId must not be null");
            Objects.requireNonNull(modelYear, "modelYear must not be null");
            Objects.requireNonNull(registeredAt, "registeredAt must not be null");
            Objects.requireNonNull(updatedAt, "updatedAt must not be null");

            return new Vehicle(this);
        }
    }
}
