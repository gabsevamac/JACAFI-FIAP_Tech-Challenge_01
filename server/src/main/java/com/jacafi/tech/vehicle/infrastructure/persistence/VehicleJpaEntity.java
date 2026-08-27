package com.jacafi.tech.vehicle.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.jacafi.tech.shared.adapter.out.persistence.AuditableJpaEntity;
import com.jacafi.tech.shared.lgpd.PersonalData;

/**
 * Storage shape of a vehicle. Deliberately separate from the aggregate: {@code domain/} may not
 * import {@code jakarta.persistence}, so the ORM mapping lives here and a mapper moves state
 * across. The boilerplate is the price of that boundary.
 *
 * <p>A JPA entity cannot be a record — the specification requires a no-args constructor and
 * non-final fields — so this is a plain class. It has no setters either, and {@link #applyState}
 * is the reason it does not need them: state moves in one call, so "partially updated row" is
 * never a representable state.
 */
@Entity
@Table(name = "vehicles")
public class VehicleJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Holds the plate while the vehicle is active and an irreversible token afterwards, which is
     * why it is wider than the seven characters a plate needs.
     */
    @PersonalData("LGPD Art. 5 I — cleared on removal, replaced by an irreversible token")
    @Column(name = "license_plate", nullable = false, length = 64)
    private String licensePlate;

    @Column(name = "make", nullable = false, length = 60)
    private String make;

    @Column(name = "model", nullable = false, length = 60)
    private String model;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    /**
     * Required by JPA, which instantiates entities reflectively before populating their state.
     * Kept {@code protected} so only Hibernate and the mapper in this package can reach it.
     */
    protected VehicleJpaEntity() {}

    VehicleJpaEntity(
            UUID id,
            String licensePlate,
            String make,
            String model,
            int modelYear,
            UUID customerId,
            Instant removedAt,
            String removedBy) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.customerId = customerId;
        // createdAt e updatedAt nao aparecem aqui: quem os escreve e o AuditingEntityListener, a
        // partir do mesmo Clock que o agregado usa. Recebe-los pelo construtor daria dois donos
        // do mesmo numero, e o listener venceria de qualquer forma — um parametro que nao decide
        // nada e pior que a ausencia dele.
        //
        // A remocao vem por restoreDeletion, e nao por markDeleted, porque este construtor tanto
        // cria quanto reconstroi uma linha ja removida, e markDeleted recusa a segunda chamada.
        restoreDeletion(removedAt, removedBy);
    }

    UUID getId() {
        return id;
    }

    String getLicensePlate() {
        return licensePlate;
    }

    String getMake() {
        return make;
    }

    String getModel() {
        return model;
    }

    int getModelYear() {
        return modelYear;
    }

    UUID getCustomerId() {
        return customerId;
    }

    /**
     * The aggregate calls this moment "registered at" and the schema calls it "created at". Same
     * instant, two vocabularies: §9 of the dictionary fixes {@code VehicleRegistered} as the
     * domain event, while the audit columns are named identically across every table so that a
     * query spanning slices does not have to learn four names for one concept. The bridge is
     * here, in the storage shape, which is the only place that has to know both.
     */
    Instant getRegisteredAt() {
        return getCreatedAt();
    }

    Instant getRemovedAt() {
        return getDeletedAt().orElse(null);
    }

    /**
     * Overwrites the mutable state of an already-managed row, in one call.
     *
     * <p>Exists because of {@code @Version}. Merging a freshly built detached instance — which is
     * what this adapter used to do — presents version 0 on every write, so the second write of a
     * row fails with {@code StaleObjectStateException} against its own earlier one. Optimistic
     * locking only works if the version travels with the row, and the alternative to this method
     * is putting the version in the aggregate, which would make the domain carry a persistence
     * concern.
     *
     * <p>Copying into the managed instance also gives the locking its correct meaning: Hibernate
     * checks the version at flush against the database, so what conflicts is another transaction's
     * write, not this transaction's own previous one.
     *
     * <p>Identifier and creation columns are absent on purpose: they do not change.
     */
    void applyState(String licensePlate, String make, String model, int modelYear, Instant deletedAt) {
        this.licensePlate = licensePlate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        restoreDeletion(deletedAt, getDeletedBy().orElse(null));
    }

    /** Identity-based equality: field-based equality breaks for managed entities. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleJpaEntity other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }

    /** Masked, for the same reason the aggregate's is: this object reaches logs too. */
    @Override
    public String toString() {
        return "VehicleJpaEntity[id=%s, licensePlate=%s, removed=%s]"
                .formatted(id, com.jacafi.tech.vehicle.domain.LicensePlate.mask(licensePlate), isDeleted());
    }
}
