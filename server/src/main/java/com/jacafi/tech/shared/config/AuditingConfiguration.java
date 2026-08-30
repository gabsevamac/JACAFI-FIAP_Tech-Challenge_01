package com.jacafi.tech.shared.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.jacafi.tech.shared.adapter.out.persistence.JwtAuditorAware;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "clockDateTimeProvider", auditorAwareRef = "jwtAuditorAware")
public class AuditingConfiguration {

    @Bean
    public DateTimeProvider clockDateTimeProvider(Clock clock) {
        return new ClockDateTimeProvider(clock);
    }

    @Bean
    public AuditorAware<String> jwtAuditorAware() {
        return new JwtAuditorAware();
    }
}
