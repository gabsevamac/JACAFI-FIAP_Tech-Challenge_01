package com.jacafi.tech.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Whoever the workshop bills for the work: a natural person or a company, told apart by which
 * {@link TaxId} they carry.
 *
 * <p>Holds its own fiscal and contact data. The previous model split it in two, with a
 * {@code Party} entity for the legal person and this one for the customer role — Fowler's Party
 * pattern, which pays for itself when the same legal person is also a supplier or an employee.
 * §2 puts purchasing, suppliers and payroll outside this bounded context, so no second role exists
 * or is planned: the split was one abstraction with one implementation, and a join for it.
 *
 * <p>A JPA entity cannot be a record — the specification requires a no-args constructor and
 * non-final fields — so this is a plain class with explicit accessors and no setters.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * Fiscal identity, and immutable: correcting a registration recorded wrongly is a different
     * act from updating a customer, with different consequences for anything already issued
     * against it.
     */
    @Column(name = "tax_id", nullable = false, updatable = false, length = 14)
    private TaxId taxId;

    @Column(nullable = false, length = 150)
    private String name;

    /** Only a legal entity has one. Enforced in {@link #applyName}. */
    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Required by JPA, which instantiates entities reflectively before populating their state.
     * Kept {@code protected} so application code has to go through {@link #create}, and its
     * invariants.
     */
    protected Customer() {
    }

    public static Customer create(TaxId taxId, String name, String tradeName, String email, String phone) {
        var customer = new Customer();
        customer.taxId = Objects.requireNonNull(taxId, "Tax id must not be null");
        customer.applyName(name, tradeName);
        customer.updateContactInformation(email, phone);
        customer.active = true;
        return customer;
    }

    /** Corrects how the customer is called, never which registration they hold. */
    public void updateName(String name, String tradeName) {
        applyName(name, tradeName);
    }

    public void updateContactInformation(String email, String phone) {
        this.email = requireText(email, "Email");
        this.phone = requireText(phone, "Phone");
    }

    public void deactivate() {
        active = false;
    }

    private void applyName(String name, String tradeName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        var normalizedTradeName = tradeName == null || tradeName.isBlank() ? null : tradeName.trim();
        // A trade name belongs to an organization. Reading it off the type of the registration is
        // the whole point of TaxId being sealed: there is no separate flag that could disagree.
        if (normalizedTradeName != null && !(taxId instanceof Cnpj)) {
            throw new IllegalArgumentException("Trade name is only allowed for legal entities");
        }

        this.name = name.trim();
        this.tradeName = normalizedTradeName;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public TaxId getTaxId() {
        return taxId;
    }

    public String getName() {
        return name;
    }

    public String getTradeName() {
        return tradeName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Identifier and state only. Name, e-mail, phone and registration all identify a person
     * (LGPD Art. 5 I) and stay out of logs and stack traces (Art. 6 VII); the registration appears
     * masked, which is enough to correlate two lines about the same customer.
     */
    @Override
    public String toString() {
        return "Customer[id=%s, taxId=%s, active=%s]"
                .formatted(id, taxId == null ? "***" : taxId.masked(), active);
    }
}
