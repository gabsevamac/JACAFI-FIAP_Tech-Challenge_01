package com.jacafi.tech.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.jacafi.tech.shared.web.ErrorCode;
import com.jacafi.tech.shared.web.TraceIdFilter;

/**
 * Renders authentication and authorization failures as RFC 7807 {@code application/problem+json}.
 *
 * <p>Without an explicit entry point, Spring Security falls back to
 * {@code Http403ForbiddenEntryPoint} for a request that carries no credentials, answering 403
 * where the correct status is 401: the client is unauthenticated, not forbidden.
 *
 * <p>The body is written by hand instead of through an {@code ObjectMapper} because Jackson 2 and
 * Jackson 3 are both on the classpath (Spring Boot 4 uses Jackson 3; {@code jjwt-jackson} still
 * pulls Jackson 2), and a security filter is the wrong place to depend on which one wins.
 *
 * <p>No detail derived from the credentials reaches the response: an error message must never
 * carry personal data (LGPD Art. 6 VII).
 */
@Component
public class SecurityProblemDetailHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        write(request, response, ErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        write(request, response, ErrorCode.ACCESS_DENIED);
    }

    /**
     * Same shape the advice produces, {@code code} and {@code traceId} included.
     *
     * <p>These two responses are the ones an unauthenticated caller sees most, so a client that
     * branches on {@code code} must be able to branch on them too. A contract that holds for every
     * error except the two most common ones is not a contract.
     */
    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        HttpStatus status = code.status();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                        {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s",\
                        "code":"%s","traceId":"%s"}""".formatted(
                        status.getReasonPhrase(),
                        status.value(),
                        escape(code.message()),
                        escape(request.getRequestURI()),
                        code.code(),
                        escape(TraceIdFilter.currentTraceId())));
    }

    /** Minimal JSON string escaping — the request URI is client-controlled input. */
    private static String escape(String value) {
        var out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append("\\u%04x".formatted((int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
