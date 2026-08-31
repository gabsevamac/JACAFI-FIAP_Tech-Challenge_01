package com.jacafi.tech.shared.time;

import java.time.ZoneOffset;
import java.util.TimeZone;

public final class ApplicationTimeZone {

    private ApplicationTimeZone() {}

    public static void enforceUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
    }
}
