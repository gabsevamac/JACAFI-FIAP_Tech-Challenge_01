package com.jacafi.tech.shared.lgpd;

public final class Masker {

    private static final String FULLY_MASKED = "***";

    private static final int KEPT = 3;

    private static final int SHORTEST_PARTIALLY_MASKABLE = 2 * KEPT;

    private static final int LONGEST_MASKED_RUN = 11;

    private Masker() {}

    public static String licensePlate(String value) {
        return maskAllBut(value, KEPT, true);
    }

    public static String document(String value) {
        return maskAllBut(value, KEPT, false);
    }

    public static String email(String value) {
        if (isUnmaskable(value)) {
            return FULLY_MASKED;
        }

        int at = value.indexOf('@');

        if (at <= 0 || at == value.length() - 1) {
            return FULLY_MASKED;
        }

        return value.charAt(0) + FULLY_MASKED + value.substring(at);
    }

    private static boolean isUnmaskable(String value) {
        return value == null || value.isBlank() || value.length() < SHORTEST_PARTIALLY_MASKABLE;
    }

    private static String maskAllBut(String value, int kept, boolean fromStart) {
        if (isUnmaskable(value)) {
            return FULLY_MASKED;
        }

        String asterisks = "*".repeat(Math.min(value.length() - kept, LONGEST_MASKED_RUN));
        return fromStart ? value.substring(0, kept) + asterisks : asterisks + value.substring(value.length() - kept);
    }
}
