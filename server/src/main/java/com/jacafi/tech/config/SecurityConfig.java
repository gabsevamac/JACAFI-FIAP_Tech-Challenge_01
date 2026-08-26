package com.jacafi.tech.config;

import java.util.List;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.jacafi.tech.auth.JwtAuthenticationFilter;

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
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(List.of(provider));
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
                        .requestMatchers("/auth/**")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        // API documentation is a project requirement, and the Swagger UI has to
                        // fetch its own JSON before any token exists.
                        // TODO: restrict these paths outside development — the MVP has a single environment.
                        .requestMatchers(SPRINGDOC_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/employee")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/employee/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
