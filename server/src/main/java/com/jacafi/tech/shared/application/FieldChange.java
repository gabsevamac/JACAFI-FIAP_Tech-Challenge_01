package com.jacafi.tech.shared.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One field of one aggregate changing value, with who changed it and when.
 *
 * <p>Field-level rather than operation-level on purpose. An entry saying "vehicle updated" cannot
 * answer the question the trail exists for — hot spots HS7 to HS10 record that the group needs to
 * recover which plate a vehicle carried on a given date, and that is not derivable from knowing
 * that an update happened.
 *
 * <p><strong>{@code oldValue} and {@code newValue} carry personal data.</strong> The audited
 * fields include the license plate and the taxpayer registration, and they are stored intact:
 * a trail recording that "the plate changed from *** to ***" answers nothing. Retained under
 * LGPD Art. 16 I. They must never reach a log or an API response without passing through
 * {@code Masker} first.
 *
 * @param aggregateType the aggregate's simple name, e.g. {@code Vehicle}
 * @param aggregateId   which instance changed
 * @param fieldName     the domain field name, not the column name
 * @param oldValue      value before the change; null when the field had none
 * @param newValue      value after the change; null when the field no longer has one
 * @param reason        why, when the use case knows; null is a legitimate answer, see below
 * @param changedAt     read from the application clock, never from the system clock
 * @param changedBy     the authenticated subject, or "system"
 */
public record FieldChange(
        String aggregateType,
        UUID aggregateId,
        String fieldName,
        String oldValue,
        String newValue,
        String reason,
        Instant changedAt,
        String changedBy) {

    public FieldChange {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        aggregateType = requireText(aggregateType, "aggregateType");
        fieldName = requireText(fieldName, "fieldName");
        changedBy = requireText(changedBy, "changedBy");

        // Null and blank are not the same answer and must not collapse into one. A blank reason
        // is a form field nobody filled in; a null reason is the use case stating that it has no
        // reason to record. HS9 keeps the semantics of "reason" open, so the distinction is the
        // only thing preserving what was actually known at the time.
        if (reason != null && reason.isBlank()) {
            throw new IllegalArgumentException("reason must be null or meaningful, never blank");
        }

        if (Objects.equals(oldValue, newValue)) {
            throw new IllegalArgumentException("a field change must change the field: old and new values are equal");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
