package com.jacafi.tech.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.jacafi.tech.auth.adapter.in.security.JwtAuthenticationFilter;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;

@Configuration
public class SecurityConfig {

    /** Paths served by springdoc: the OpenAPI 3.1 document and the Swagger UI. */
    private static final String[] SPRINGDOC_PATHS = {
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            SecurityProblemDetailHandler problemDetailHandler)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Without an explicit entry point, a request with no credentials got 403 instead of 401.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        // API documentation is a project requirement, and the Swagger UI has to
                        // fetch its own JSON before any token exists.
                        // TODO: restrict these paths outside development — the MVP has a single environment.
                        .requestMatchers(SPRINGDOC_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/user-accounts/me")
                        .authenticated()
                        .requestMatchers("/api/v1/user-accounts", "/api/v1/user-accounts/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/customers/me")
                        .authenticated()
                        .requestMatchers("/api/v1/customers", "/api/v1/customers/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SERVICE_ADVISOR")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
