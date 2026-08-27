package com.jacafi.tech.shared.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.jacafi.tech.shared.config.ClockDateTimeProvider;

/**
 * Technical audit columns, inherited by every persisted business entity.
 *
 * <p>Answers "where did the current state of this row come from": when it was written, by whom,
 * and whether it has been logically removed. That is a different question from the one the audit
 * trail answers — {@code updatedBy} holds only the most recent author and overwrites the previous
 * one on every write, so it is a pointer to the last change, not a history of changes.
 *
 * <p>Lives in {@code infrastructure} by necessity: this class carries JPA annotations, and the
 * domain packages may not. Aggregates stay free of {@code jakarta.persistence}, and the storage
 * shape inherits from here.
 *
 * <p>Timestamps are {@link Instant} against {@code TIMESTAMPTZ}. Both are absolute, so no zone
 * conversion happens and the value cannot depend on the machine that wrote it.
 *
 * <p>The values come from {@code AuditingEntityListener}, driven by the application {@code Clock}
 * through {@link ClockDateTimeProvider} — not from the system clock. That is what lets an
 * integration test assert an equality on {@code createdAt} instead of a tolerance.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableJpaEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 120)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 120)
    private String updatedBy;

    /**
     * Filled by hand on logical removal, not by a listener.
     *
     * <p>There is no {@code @DeletedDate} to hang a listener on, and inventing one would be worse
     * than the explicit call: removal is a business decision — the aggregate decides whether it is
     * allowed — while created and modified are consequences of writing a row. Conflating them
     * would let any save mark a record removed.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

    /**
     * Optimistic locking.
     *
     * <p>Without it, two screens open on the same service order overwrite each other and the last
     * write wins silently. In an approval flow that means recording approval of a total the
     * customer never saw — the invariant that no service runs without a registered approval,
     * defeated by a race rather than by a bug in the rule.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    /** Empty while the row is active. */
    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public Optional<String> getDeletedBy() {
        return Optional.ofNullable(deletedBy);
    }

    public long getVersion() {
        return version;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Marks the row logically removed. Idempotent by refusal rather than by silence: calling it
     * twice is a bug in the caller, and the second call would otherwise overwrite the first
     * author and moment.
     */
    public void markDeleted(Instant at, String by) {
        if (deletedAt != null) {
            throw new IllegalStateException("Row is already marked as deleted.");
        }
        this.deletedAt = at;
        this.deletedBy = by;
    }

    /** For the persistence mapper, which rebuilds a stored row from the aggregate. */
    protected void restoreDeletion(Instant at, String by) {
        this.deletedAt = at;
        this.deletedBy = by;
    }
}
