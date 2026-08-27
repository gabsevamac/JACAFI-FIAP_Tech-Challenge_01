package com.jacafi.tech.customer.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Customer {

    private final UUID id;
    private final TaxId taxId;
    private final long version;
    private final Instant createdAt;
    private final Instant updatedAt;
    private String name;
    private String tradeName;
    private String email;
    private String phone;
    private boolean active;

    private Customer(
            UUID id,
            TaxId taxId,
            String name,
            String tradeName,
            String email,
            String phone,
            boolean active,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.taxId = Objects.requireNonNull(taxId, "taxId must not be null");
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        changeProfile(name, tradeName, email, phone);
        this.active = active;
    }

    public static Customer register(TaxId taxId, String name, String tradeName, String email, String phone) {
        return new Customer(UUID.randomUUID(), taxId, name, tradeName, email, phone, true, 0, null, null);
    }

    public static Customer restore(
            UUID id,
            TaxId taxId,
            String name,
            String tradeName,
            String email,
            String phone,
            boolean active,
            long version,
            Instant createdAt,
            Instant updatedAt) {
        return new Customer(id, taxId, name, tradeName, email, phone, active, version, createdAt, updatedAt);
    }

    public void changeProfile(String name, String tradeName, String email, String phone) {
        this.name = requiredText(name, "name");
        this.tradeName = normalizeTradeName(tradeName);
        this.email = requiredText(email, "email");
        this.phone = requiredText(phone, "phone");
    }

    public void deactivate() {
        active = false;
    }

    public UUID id() {
        return id;
    }

    public TaxId taxId() {
        return taxId;
    }

    public String name() {
        return name;
    }

    public String tradeName() {
        return tradeName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public boolean active() {
        return active;
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

    @Override
    public String toString() {
        return "Customer[id=%s, taxId=%s, active=%s]".formatted(id, taxId.masked(), active);
    }

    private String normalizeTradeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!(taxId instanceof Cnpj)) {
            throw new IllegalArgumentException("tradeName is only allowed for legal entities");
        }
        return value.trim();
    }

    private static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
