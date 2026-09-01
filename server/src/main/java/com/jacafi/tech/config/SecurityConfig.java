package com.jacafi.tech.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

import com.jacafi.tech.shared.adapter.in.security.KeycloakJwtAuthenticationConverter;
import com.jacafi.tech.shared.adapter.in.security.SecurityContextCurrentAuthenticatedUserAdapter;
import com.jacafi.tech.shared.adapter.in.web.SecurityProblemDetailHandler;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.CustomerIdentityPort;

@Configuration
public class SecurityConfig {

    private static final String[] SPRINGDOC_PATHS = {
        "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"
    };

    private static final String EMPLOYEE = "EMPLOYEE";

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(CustomerIdentityPort customerIdentities) {
        return new KeycloakJwtAuthenticationConverter(customerIdentities);
    }

    @Bean
    CurrentAuthenticatedUserPort currentAuthenticatedUserPort() {
        return new SecurityContextCurrentAuthenticatedUserAdapter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
            SecurityProblemDetailHandler problemDetailHandler)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers(SPRINGDOC_PATHS)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/customers/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/vehicles/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/service-orders/*/status")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/service-orders/*/estimates/*/decision")
                        .authenticated()
                        .anyRequest()
                        .hasRole(EMPLOYEE))
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(problemDetailHandler)
                        .accessDeniedHandler(problemDetailHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }
}
