package com.jacafi.tech.shared.application;

/**
 * Port for appending to the audit trail.
 *
 * <p>Append-only by design: no method reads, amends or deletes an entry. A trail whose entries can
 * be rewritten is not evidence of anything, and offering the method at all invites the first
 * "just fix that one row" that ends the trail's usefulness.
 *
 * <p>Called from the use case, not from a JPA listener. Only the use case knows <em>why</em> a
 * field changed, and the reason is the part a listener could never supply — a listener sees a
 * plate go from one value to another and cannot tell a typo correction from a re-plating, which
 * is exactly the distinction HS9 is about.
 *
 * <p>Lives in the application layer of {@code shared} and names no persistence type, so a slice
 * can depend on it without depending on JPA.
 */
public interface AuditTrailPort {

    void record(FieldChange change);
}
