package com.jacafi.tech.features.service.domain;

import com.jacafi.tech.features.service_order.domain.ServiceOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain entity representing a type of mechanical/technical service offered by the workshop.
 * Acts as a catalogue item that can be associated with {@link ServiceOrder}s.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Required by JPA; hidden from application code
@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "description", nullable = false, length = 45)
    private String description;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    /**
     * Factory method — the only public way to create a new {@link Service}.
     * Enforces invariants before persistence.
     *
     * @param description human-readable service description
     * @param basePrice   the reference price for this service (must be non-negative)
     * @return a new, unpersisted {@link Service} instance
     */
    public static Service create(String description, BigDecimal basePrice) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Service description must not be blank");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Service base price must be zero or positive");
        }
        var service = new Service();
        service.description = description;
        service.basePrice = basePrice;
        return service;
    }

    /**
     * Updates the base price of the service.
     * Encapsulates the mutation so that domain rules can be enforced.
     *
     * @param newPrice the new base price (must be non-negative)
     */
    public void updateBasePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Service base price must be zero or positive");
        }
        this.basePrice = newPrice;
    }
}
