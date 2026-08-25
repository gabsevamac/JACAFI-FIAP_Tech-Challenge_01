package com.jacafi.tech.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "parties")
public class Party {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Embedded
    private TaxIdentifier taxIdentifier;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Party create(String name, String tradeName, TaxIdentifier taxIdentifier) {
        var party = new Party();
        party.taxIdentifier = Objects.requireNonNull(taxIdentifier, "Tax identifier must not be null");
        party.updateName(name, tradeName);
        return party;
    }

    public void updateName(String name, String tradeName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }

        var normalizedTradeName = tradeName == null || tradeName.isBlank() ? null : tradeName.trim();
        if (taxIdentifier.getPersonType() == PersonType.INDIVIDUAL && normalizedTradeName != null) {
            throw new IllegalArgumentException("Trade name is only allowed for legal entities");
        }

        this.name = name.trim();
        this.tradeName = normalizedTradeName;
    }

    public PersonType getPersonType() {
        return taxIdentifier.getPersonType();
    }
}
