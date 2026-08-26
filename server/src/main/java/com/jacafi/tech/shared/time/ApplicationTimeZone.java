package com.jacafi.tech.shared.time;

import java.time.ZoneOffset;
import java.util.TimeZone;

/**
 * Forces the JVM default time zone to UTC.
 *
 * <p>Everything the application writes is already UTC by construction: the domain speaks {@link
 * java.time.Instant}, the columns are {@code TIMESTAMPTZ}, and Hibernate is pinned by
 * {@code hibernate.jdbc.time_zone}. What remains is the code that never asked about a time zone
 * and gets the platform default handed to it anyway — a {@code DateTimeFormatter} without a zone,
 * a library that builds a {@code java.util.Date}, a log appender rendering a timestamp.
 *
 * <p>The container sets {@code TZ=UTC} and covers the deployed case. This covers the other one:
 * a developer running the application straight from the IDE, on a laptop in
 * {@code America/Sao_Paulo}, three hours away from what the container would have produced. That
 * gap is where a bug hides best, because it only appears on someone else's machine.
 *
 * <p>Called from {@code main} before Spring starts, so that no bean can read the default before
 * it is correct.
 */
public final class ApplicationTimeZone {

    private ApplicationTimeZone() {}

    /**
     * Sets the JVM-wide default to UTC.
     *
     * <p>Global mutable state, set once, on purpose. The alternative — auditing every call that
     * might read the default — is not a thing a group of four can keep up with across four
     * parallel slices.
     */
    public static void enforceUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }
}
