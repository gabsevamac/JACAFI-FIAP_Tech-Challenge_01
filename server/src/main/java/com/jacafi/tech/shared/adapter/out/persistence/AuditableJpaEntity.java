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

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 120)
    private String deletedBy;

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

    public void markDeleted(Instant at, String by) {
        if (deletedAt != null) {
            throw new IllegalStateException("Row is already marked as deleted.");
        }
        this.deletedAt = at;
        this.deletedBy = by;
    }

    protected void restoreDeletion(Instant at, String by) {
        this.deletedAt = at;
        this.deletedBy = by;
    }
}
