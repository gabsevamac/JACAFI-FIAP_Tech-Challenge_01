package com.jacafi.tech.shared.lgpd;

/**
 * Renders personal data in a form that is safe to write down.
 *
 * <p>License plates, taxpayer registrations and e-mail addresses are personal data under LGPD
 * Art. 5 I, and Art. 6 VII — the security principle — keeps them out of logs, error messages and
 * stack traces. That obligation does not disappear when a log line is useful, so the answer is
 * not to log nothing: it is to log a rendering that supports an investigation without identifying
 * anyone.
 *
 * <p>Every method keeps a fixed number of characters and replaces the rest. The kept characters
 * are what let a human correlate two entries about the same subject; the replaced ones are what
 * stop the entry from being the data itself. Which end is kept differs by type, and each method
 * says why.
 *
 * <p>Shared rather than per-slice on purpose. Four slices each masking to their own taste
 * produces a log where the same vehicle reads two ways, which defeats the correlation the masking
 * was kept partial to allow.
 *
 * <p>None of these throw. A masking helper that can fail is a masking helper that gets wrapped in
 * a try/catch and eventually skipped, and the failure mode of skipping it is the full value in
 * the log.
 */
public final class Masker {

    /**
     * Stand-in for a value that cannot be masked meaningfully — null, blank, or short enough that
     * keeping any part of it would keep all of it. Deliberately not the empty string: a log line
     * reading {@code plate=} is ambiguous between "absent" and "masking failed", and
     * {@code plate=***} is not.
     */
    private static final String FULLY_MASKED = "***";

    private static final int KEPT = 3;

    /**
     * Below this length, partial masking would hide less than it reveals — at five characters,
     * keeping three leaves two. The threshold is {@code 2 * KEPT} so that a partially masked value
     * is always at least half hidden.
     */
    private static final int SHORTEST_PARTIALLY_MASKABLE = 2 * KEPT;

    /**
     * Upper bound on the run of asterisks.
     *
     * <p>The mask is otherwise proportional, which keeps the length of the original — deliberate
     * for the formats this handles, since a CPF and a CNPJ are already told apart by their length
     * and nothing further is disclosed by preserving it. Eleven is what a CNPJ needs, and a CNPJ
     * is the longest of them.
     *
     * <p>Anything longer than that is not one of these formats. The removal token that replaces a
     * plate is forty-one characters, and reproducing its length gave a log line with thirty-eight
     * asterisks in it — disclosing the length of a value, for no benefit, while being unreadable.
     */
    private static final int LONGEST_MASKED_RUN = 11;

    private Masker() {}

    /**
     * Keeps the three leading characters: {@code ABC1D23} becomes {@code ABC****}.
     *
     * <p>The leading end because that is the part shared by both Brazilian layouts — the
     * pre-Mercosur {@code ABC1234} and the Mercosur {@code ABC1D23} open with the same three
     * letters. Masking the same positions in both means a plate cannot be classified by looking at
     * which characters survived.
     *
     * <p>Takes a raw string rather than the plate type, because the values most in need of masking
     * are the ones that failed validation and therefore never became an instance of it.
     *
     * @param value any value that might be a plate; may be null
     * @return the masked rendering, never null and never the full value
     */
    public static String licensePlate(String value) {
        return maskAllBut(value, KEPT, true);
    }

    /**
     * Keeps the three trailing characters: the CPF {@code 52998224725} becomes {@code ********725}.
     *
     * <p>The trailing end, unlike the plate, and for a specific reason: the leading digits of a
     * CPF are not random. They are assigned in blocks that correlate with the region where the
     * registration was issued, so keeping the head would disclose an attribute of the person on
     * top of the digits themselves. The final digits include the two check digits, which are
     * derived from the rest and therefore carry the least independent information.
     *
     * <p>Serves CPF and CNPJ through one method because the caller should not have to decide, and
     * because a caller that decides wrongly gets the wrong masking.
     *
     * @param value any value that might be a registration; may be null
     * @return the masked rendering, never null and never the full value
     */
    public static String document(String value) {
        return maskAllBut(value, KEPT, false);
    }

    /**
     * Keeps the first character and the whole domain: {@code mariana@example.com} becomes
     * {@code m***@example.com}.
     *
     * <p>The domain survives because it is what makes the line useful — it identifies the tenant,
     * the corporate account, the provider — and because it describes an organization rather than a
     * person. The local part is the identifying half and is replaced by a fixed run of asterisks,
     * not one per character: a proportional mask would disclose the length, which narrows a guess
     * considerably for a name-shaped address.
     *
     * @param value any value that might be an address; may be null
     * @return the masked rendering, never null and never the full local part
     */
    public static String email(String value) {
        if (isUnmaskable(value)) {
            return FULLY_MASKED;
        }

        int at = value.indexOf('@');

        // No separator, nothing before it, or nothing after it: not an address this method can
        // reason about, and guessing which half is the domain would be how a domain ends up
        // treated as a local part. Mask the lot.
        if (at <= 0 || at == value.length() - 1) {
            return FULLY_MASKED;
        }

        return value.charAt(0) + FULLY_MASKED + value.substring(at);
    }

    private static boolean isUnmaskable(String value) {
        return value == null || value.isBlank() || value.length() < SHORTEST_PARTIALLY_MASKABLE;
    }

    /**
     * @param fromStart whether the kept characters are the leading ones or the trailing ones
     */
    private static String maskAllBut(String value, int kept, boolean fromStart) {
        if (isUnmaskable(value)) {
            return FULLY_MASKED;
        }

        String asterisks = "*".repeat(Math.min(value.length() - kept, LONGEST_MASKED_RUN));
        return fromStart ? value.substring(0, kept) + asterisks : asterisks + value.substring(value.length() - kept);
    }
}
