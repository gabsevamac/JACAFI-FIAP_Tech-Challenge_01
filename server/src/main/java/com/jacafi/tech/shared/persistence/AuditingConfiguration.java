package com.jacafi.tech.shared.persistence;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.jacafi.tech.shared.security.JwtAuditorAware;

/**
 * Turns on JPA auditing for every entity inheriting {@link AuditableEntity}.
 *
 * <p>Both references are named explicitly rather than left to type resolution. Spring Data picks
 * a {@code DateTimeProvider} by bean name, and the default name resolves to one backed by the
 * system clock — so omitting {@code dateTimeProviderRef} produces auditing that silently ignores
 * the application clock, with no error to notice.
 */
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
