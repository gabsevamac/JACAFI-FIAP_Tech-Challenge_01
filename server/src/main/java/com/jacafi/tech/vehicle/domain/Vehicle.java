package com.jacafi.tech.vehicle.domain;

import com.jacafi.tech.shared.lgpd.PersonalData;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A vehicle presented to the workshop for maintenance, described by make, model and model year.
 *
 * <p>Aggregate root. It carries a surrogate identity ({@code id}) for references and a business
 * identity (its {@link LicensePlate}): the workshop recognizes a returning vehicle by the plate,
 * which is why a known vehicle is linked to a new visit rather than registered again.
 *
 * <p>Pure domain: no framework, no ORM, no HTTP. The only non-JDK import is the personal data
 * marker, which is an annotation and carries no behaviour. Time arrives as a {@link Clock}
 * parameter instead of being read from the ambient system, so that every rule involving dates is
 * deterministic under test.
 *
 * <p>No setters. State changes go through methods named after what the workshop actually does,
 * and each of them enforces the invariants that guard the change.
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
    private final CustomerId customerId;
    private final Instant registeredAt;
    private Instant updatedAt;
    private Instant removedAt;

    private Vehicle(UUID id, LicensePlate licensePlate, String make, String model, int modelYear,
                    CustomerId customerId, Instant registeredAt, Instant updatedAt, Instant removedAt) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.customerId = customerId;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.removedAt = removedAt;
    }

    /**
     * Registers a vehicle for a customer.
     *
     * <p>The identifier is supplied by the caller rather than generated here: a domain object that
     * invents its own random identity cannot be asserted on in a test, and the application layer
     * needs the identifier anyway, to write the audit entry.
     *
     * <p>Plate uniqueness is <em>not</em> checked here. An aggregate can only enforce invariants
     * over its own state, and uniqueness spans the whole collection — that check belongs to the
     * application layer, which owns the repository.
     *
     * @throws InvalidLicensePlateException from {@link LicensePlate} when the format is rejected
     * @throws IllegalArgumentException     when a required attribute is missing or out of range
     */
    public static Vehicle register(UUID id,
                                   LicensePlate licensePlate,
                                   String make,
                                   String model,
                                   int modelYear,
                                   CustomerId customerId,
                                   Clock clock) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(licensePlate, "licensePlate must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        Instant now = clock.instant();
        return new Vehicle(id,
                licensePlate,
                requireText(make, "make"),
                requireText(model, "model"),
                requireModelYearInRange(modelYear, clock),
                customerId,
                now,
                now,
                null);
    }

    /**
     * Rebuilds an aggregate already stored, for the persistence layer only.
     *
     * <p>Business rules are not re-applied: this data was validated when it was first accepted,
     * and re-running the checks would let a rule introduced today reject a row written yesterday.
     * A removed vehicle arrives here with no plate and a {@code removedAt}, which is a legitimate
     * state to rebuild and an impossible one to register.
     */
    public static Vehicle restore(UUID id,
                                  LicensePlate licensePlate,
                                  String make,
                                  String model,
                                  int modelYear,
                                  CustomerId customerId,
                                  Instant registeredAt,
                                  Instant updatedAt,
                                  Instant removedAt) {
        return new Vehicle(Objects.requireNonNull(id, "id must not be null"),
                licensePlate,
                make,
                model,
                modelYear,
                Objects.requireNonNull(customerId, "customerId must not be null"),
                Objects.requireNonNull(registeredAt, "registeredAt must not be null"),
                Objects.requireNonNull(updatedAt, "updatedAt must not be null"),
                removedAt);
    }

    /**
     * Corrects the descriptive attributes of the vehicle.
     *
     * <p>The license plate is absent from this signature on purpose: it is the vehicle's business
     * identity and is immutable after registration.
     *
     * <p>TODO: correcting a plate that was recorded wrongly is a use case of its own, with its own
     * audit entry and its own authorization — not a field on this method. Out of scope for the MVP.
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

    public CustomerId getCustomerId() {
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
}
