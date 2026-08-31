package com.jacafi.tech.shared.adapter.in.web;

public final class LogSafe {

    private static final int MAX_LENGTH = 500;

    private LogSafe() {}

    public static String value(String raw) {
        if (raw == null) {
            return "";
        }

        String trimmed = raw.length() > MAX_LENGTH ? raw.substring(0, MAX_LENGTH) + "…[truncado]" : raw;

        return trimmed.replace('\n', '⏎').replace('\r', '⏎').replace('\t', ' ');
    }
}
