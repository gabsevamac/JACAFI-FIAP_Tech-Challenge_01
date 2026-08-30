package com.jacafi.tech.shared.adapter.out.persistence;

public enum OutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED
}
