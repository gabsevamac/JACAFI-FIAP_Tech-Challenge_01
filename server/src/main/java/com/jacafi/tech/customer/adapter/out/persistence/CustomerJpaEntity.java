package com.jacafi.tech.customer.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "customers")
public class CustomerJpaEntity extends AuditableJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tax_id", nullable = false, updatable = false, length = 14)
    private String taxId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active;

    protected CustomerJpaEntity() {}

    CustomerJpaEntity(
            UUID id, String taxId, String name, String tradeName, String email, String phone, boolean active) {
        this.id = id;
        this.taxId = taxId;
        this.name = name;
        this.tradeName = tradeName;
        this.email = email;
        this.phone = phone;
        this.active = active;
    }

    UUID id() {
        return id;
    }

    String taxId() {
        return taxId;
    }

    String name() {
        return name;
    }

    String tradeName() {
        return tradeName;
    }

    String email() {
        return email;
    }

    String phone() {
        return phone;
    }

    boolean active() {
        return active;
    }

    void apply(CustomerJpaEntity source) {
        name = source.name;
        tradeName = source.tradeName;
        email = source.email;
        phone = source.phone;
        active = source.active;
    }
}
