package com.jacafi.tech.vehicle.domain.entity;

import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Vehicle {

    private static final int EARLIEST_MODEL_YEAR = 1886;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final UUID id;
    private final LicensePlate licensePlate;
    private final UUID customerId;
    private final long version;
    private final Instant registeredAt;
    private String make;
    private String model;
    private int modelYear;
    private Instant updatedAt;
    private Instant removedAt;

    private Vehicle(
            UUID id,
            LicensePlate licensePlate,
            String make,
            String model,
            int modelYear,
            UUID customerId,
            long version,
            Instant registeredAt,
            Instant updatedAt,
            Instant removedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.licensePlate = Objects.requireNonNull(licensePlate, "licensePlate must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        this.version = version;
        this.registeredAt = Objects.requireNonNull(registeredAt, "registeredAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.removedAt = removedAt;
        this.make = requireText(make, "make");
        this.model = requireText(model, "model");
        this.modelYear = modelYear;
    }

    public static Vehicle register(
            UUID id,
            LicensePlate licensePlate,
            String make,
            String model,
            int modelYear,
            UUID customerId,
            Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        Instant now = clock.instant();
        Vehicle vehicle = new Vehicle(id, licensePlate, make, model, modelYear, customerId, 0, now, now, null);
        vehicle.requireModelYear(modelYear, clock);
        return vehicle;
    }

    public static Vehicle restore(
            UUID id,
            LicensePlate licensePlate,
            String make,
            String model,
            int modelYear,
            UUID customerId,
            long version,
            Instant registeredAt,
            Instant updatedAt,
            Instant removedAt) {
        return new Vehicle(
                id, licensePlate, make, model, modelYear, customerId, version, registeredAt, updatedAt, removedAt);
    }

    public void changeDetails(String make, String model, int modelYear, Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        requireActive();
        this.make = requireText(make, "make");
        this.model = requireText(model, "model");
        requireModelYear(modelYear, clock);
        this.modelYear = modelYear;
        updatedAt = clock.instant();
    }

    public void remove(Instant at) {
        requireActive();
        removedAt = Objects.requireNonNull(at, "removedAt must not be null");
        updatedAt = removedAt;
    }

    public UUID id() {
        return id;
    }

    public LicensePlate licensePlate() {
        return licensePlate;
    }

    public String make() {
        return make;
    }

    public String model() {
        return model;
    }

    public int modelYear() {
        return modelYear;
    }

    public UUID customerId() {
        return customerId;
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

    private void requireActive() {
        if (!active()) {
            throw new IllegalStateException("A removed vehicle cannot be changed.");
        }
    }

    private void requireModelYear(int value, Clock clock) {
        int latestModelYear = Year.now(clock).getValue() + 1;
        if (value < EARLIEST_MODEL_YEAR || value > latestModelYear) {
            throw new IllegalArgumentException(
                    "modelYear must be between %d and %d".formatted(EARLIEST_MODEL_YEAR, latestModelYear));
        }
    }

    private static String requireText(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        String normalized = WHITESPACE.matcher(value.trim()).replaceAll(" ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "Vehicle[id=%s, licensePlate=%s, active=%s]".formatted(id, licensePlate.masked(), active());
    }
}
