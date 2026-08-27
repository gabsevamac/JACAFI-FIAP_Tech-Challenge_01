package com.jacafi.tech.shared.adapter.in.web;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an opaque identifier, in the log and in the response.
 *
 * <p>This is the mechanism the whole error contract rests on. The client is told nothing about
 * what went wrong beyond a status and a generic sentence; the server log holds the stack trace,
 * the SQL, the constraint name. The trace id is the only thing linking the two — which lets
 * support answer "what happened to my request" while telling an attacker nothing, because the
 * identifier is random and means nothing without access to the log.
 *
 * <p>Highest precedence: a failure in a later filter — an unparseable JWT, for instance — must
 * still produce a response carrying the header, and a filter that has not run yet cannot add one.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";

    /** Key under which the id appears in the log pattern. */
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Deliberately not read from an inbound header. Trusting a client-supplied value would let
        // a caller choose its own identifier, and therefore collide with — or forge — another
        // caller's log entries. A gateway that needs correlation across services should send its
        // own header, kept separate from this one.
        String traceId = UUID.randomUUID().toString();

        MDC.put(MDC_KEY, traceId);
        // Set before the chain runs, not after: the response may already be committed by the time
        // control returns, and a header added to a committed response is dropped in silence.
        response.setHeader(HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            // The thread goes back to the pool. Without this, the next request served by it would
            // inherit this id until it set its own, and the log would attribute one caller's lines
            // to another's trace.
            MDC.remove(MDC_KEY);
        }
    }

    /** The current request's id, for the advice building the response body. */
    public static String currentTraceId() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "unavailable" : traceId;
    }
}
