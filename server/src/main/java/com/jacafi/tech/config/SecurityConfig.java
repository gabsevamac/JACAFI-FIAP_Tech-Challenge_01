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
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                        .permitAll()
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/me")
                        .authenticated()
                        .requestMatchers("/api/v1/vehicles", "/api/v1/vehicles/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SERVICE_ADVISOR")
                        .requestMatchers(
                                HttpMethod.GET, "/api/v1/service-catalog-items", "/api/v1/service-catalog-items/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SERVICE_ADVISOR")
                        .requestMatchers("/api/v1/service-catalog-items", "/api/v1/service-catalog-items/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/service-orders/*/status")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/service-orders/*/estimates/*/decision")
                        .authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/service-orders/*/status")
                        .hasAnyRole("ADMIN", "MANAGER", "SERVICE_ADVISOR", "TECHNICIAN")
                        .requestMatchers("/api/v1/service-orders", "/api/v1/service-orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "SERVICE_ADVISOR")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
