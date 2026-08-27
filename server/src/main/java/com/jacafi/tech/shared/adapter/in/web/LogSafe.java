package com.jacafi.tech.shared.adapter.in.web;

/**
 * Makes a client-controlled value safe to put in a log line.
 *
 * <p>Guards against log forging (CWE-117). The log pattern writes one record per line, so a value
 * containing a newline lets whoever supplied it append lines of its own — inventing a plausible
 * entry, with someone else's trace id, in the middle of a real incident. The forged lines are
 * indistinguishable from genuine ones precisely because they are genuine lines, written by the
 * logger, with content the caller chose.
 *
 * <p>Reaches the log through {@code e.getMessage()} in the error handler: several framework
 * exceptions quote the submitted value in their message, so client input arrives there without
 * anyone having logged it on purpose.
 *
 * <p>Line breaks are replaced rather than stripped, so the entry still shows that something was
 * removed instead of quietly closing up.
 */
public final class LogSafe {

    /** Beyond this, a message is being used to flood the log rather than to describe a failure. */
    private static final int MAX_LENGTH = 500;

    private LogSafe() {}

    public static String value(String raw) {
        if (raw == null) {
            return "";
        }

        String trimmed = raw.length() > MAX_LENGTH ? raw.substring(0, MAX_LENGTH) + "…[truncado]" : raw;

        // \r tambem: sozinho ele ainda quebra linha em alguns consumidores de log, e num terminal
        // reposiciona o cursor no inicio da linha, sobrescrevendo o que ja estava escrito.
        return trimmed.replace('\n', '⏎').replace('\r', '⏎').replace('\t', ' ');
    }
}
