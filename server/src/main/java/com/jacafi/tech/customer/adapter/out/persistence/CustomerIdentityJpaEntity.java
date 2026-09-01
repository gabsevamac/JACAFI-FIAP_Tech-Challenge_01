package com.jacafi.tech.customer.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;

@Entity
@Table(name = "customer_identities")
public class CustomerIdentityJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "subject_id", length = 64)
    private String subjectId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    protected CustomerIdentityJpaEntity() {}

    CustomerIdentityJpaEntity(String subjectId, UUID customerId) {
        this.subjectId = subjectId;
        this.customerId = customerId;
    }

    String subjectId() {
        return subjectId;
    }

    UUID customerId() {
        return customerId;
    }

    void moveTo(UUID customerId) {
        this.customerId = customerId;
    }
}
