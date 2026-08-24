package com.jacafi.tech.features.service_order.domain;
import com.jacafi.tech.features.service.domain.Service;
import com.jacafi.tech.features.service_order.add_service_to_order.ServiceOrderService;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "service_orders")
public class ServiceOrder {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private ServiceOrderStatus status;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    //TODO: Alterar campo quando entidade Vehicle estiver feita
    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    //TODO: Alterar campo quando entidade Client estiver feita
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @OneToMany(
            mappedBy = "serviceOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ServiceOrderService> services = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //TODO: Alterar parâmetros quando as entidades Vehicle e User estiverem feitas.
    public static ServiceOrder open(UUID vehicleId, UUID clientId) {
        if (vehicleId == null) throw new IllegalArgumentException("vehicleId must not be null");
        if (clientId == null) throw new IllegalArgumentException("clientId must not be null");

        var order = new ServiceOrder();
        order.vehicleId = vehicleId;
        order.clientId = clientId;
        order.status = ServiceOrderStatus.RECEIVED;
        order.total = BigDecimal.ZERO;
        return order;
    }

    public void startDiagnosis() {
        requireStatus(ServiceOrderStatus.RECEIVED);
        this.status = ServiceOrderStatus.IN_DIAGNOSIS;
    }

    public void awaitApproval() {
        requireStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        this.status = ServiceOrderStatus.PENDING_APPROVAL;
    }


    public void approve() {
        requireStatus(ServiceOrderStatus.PENDING_APPROVAL);
        this.status = ServiceOrderStatus.IN_PROGRESS;
    }


    public void refuse() {
        requireStatus(ServiceOrderStatus.PENDING_APPROVAL);
        this.status = ServiceOrderStatus.REJECTED;
    }

    public void finish() {
        requireStatus(ServiceOrderStatus.IN_PROGRESS);
        this.status = ServiceOrderStatus.COMPLETED;
    }

    public void deliver() {
        requireStatus(ServiceOrderStatus.COMPLETED);
        this.status = ServiceOrderStatus.DELIVERED;
    }

    public void addService(
            Service service,
            BigDecimal priceAtSale,
            int quantity) {

        requireEditable();
        var item = ServiceOrderService.create(this, service, priceAtSale, quantity);
        this.services.add(item);
        recalculateTotal();
    }


    public void removeService(UUID serviceId) {
        requireEditable();
        this.services.removeIf(item -> item.getService().getId().equals(serviceId));
        recalculateTotal();
    }


    public List<ServiceOrderService> getServices() {
        return Collections.unmodifiableList(services);
    }


    private void requireStatus(ServiceOrderStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Expected status %s but was %s".formatted(expected, this.status));
        }
    }

    private void requireEditable() {
        if (this.status == ServiceOrderStatus.COMPLETED
                || this.status == ServiceOrderStatus.DELIVERED
                || this.status == ServiceOrderStatus.REJECTED) {
            throw new IllegalStateException(
                    "Cannot modify a service order in status: " + this.status);
        }
    }

    private void recalculateTotal() {
        this.total = services.stream()
                .map(item -> item.getPriceAtSale()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
