package com.jacafi.tech.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED,
                "Authentication is required to access this resource.");
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN,
                "The authenticated principal is not allowed to perform this operation.");
    }

    private void write(HttpServletRequest request,
                       HttpServletResponse response,
                       HttpStatus status,
                       String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s"}"""
                .formatted(status.getReasonPhrase(), status.value(), detail, escape(request.getRequestURI())));
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
