package com.jacafi.tech.vehicle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.jacafi.tech.vehicle.domain.Vehicle;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Proves the requirement that no log statement writes a full license plate.
 *
 * <p>Asserted against what the logging framework actually received, not against the source: a
 * review can miss an interpolated plate, and the next person to add a log line will not have read
 * this file. The appender is attached to the root logger so a statement from anywhere in the
 * slice is caught.
 */
class LogMaskingTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-25T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID CUSTOMER = UUID.fromString("5b6c7d8e-9f0a-4b1c-8d2e-3f4a5b6c7d8e");
    private static final String PLATE = "ABC1234";
    private static final String MASKED_PLATE = "ABC***4";

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger rootLogger;

    private InMemoryVehicleRepository repository;
    private RegisterVehicleUseCase register;
    private UpdateVehicleUseCase update;
    private RemoveVehicleUseCase remove;

    @BeforeEach
    void attachAppender() {
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.TRACE);
        appender.start();
        rootLogger.addAppender(appender);

        repository = new InMemoryVehicleRepository();
        RecordingAuditTrail auditTrail = new RecordingAuditTrail();
        register = new RegisterVehicleUseCase(repository, auditTrail, CLOCK);
        update = new UpdateVehicleUseCase(repository, auditTrail, CLOCK);
        remove = new RemoveVehicleUseCase(repository, auditTrail, CLOCK);
    }

    @AfterEach
    void detachAppender() {
        rootLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("registering, updating and removing never log the full plate")
    void neverLogsTheFullPlate() {
        Vehicle vehicle = register.register(
                new RegisterVehicleCommand(PLATE, "Volkswagen", "Gol", 2020, CUSTOMER, "advisor@sinates"));
        update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, "advisor@sinates"));
        remove.remove(vehicle.getId(), "advisor@sinates");

        List<String> messages = loggedMessages();

        assertThat(messages).isNotEmpty();
        assertThat(messages).noneMatch(message -> message.contains(PLATE));
        assertThat(messages).anyMatch(message -> message.contains(MASKED_PLATE));
    }

    @Test
    @DisplayName("the masked form appears for all three operations, so each one is traceable")
    void logsTheMaskedFormForEveryWrite() {
        Vehicle vehicle = register.register(
                new RegisterVehicleCommand(PLATE, "Volkswagen", "Gol", 2020, CUSTOMER, "advisor@sinates"));
        update.update(new UpdateVehicleCommand(vehicle.getId(), "Chevrolet", "Onix", 2021, "advisor@sinates"));
        remove.remove(vehicle.getId(), "advisor@sinates");

        assertThat(loggedMessages())
                .filteredOn(message -> message.contains(MASKED_PLATE))
                .hasSize(3);
    }

    @Test
    @DisplayName("logging the aggregate itself is also safe, since toString is masked")
    void logsTheAggregateSafely() {
        Vehicle vehicle = register.register(
                new RegisterVehicleCommand(PLATE, "Volkswagen", "Gol", 2020, CUSTOMER, "advisor@sinates"));

        org.slf4j.LoggerFactory.getLogger(LogMaskingTest.class).info("vehicle={}", vehicle);

        assertThat(loggedMessages()).noneMatch(message -> message.contains(PLATE));
    }

    private List<String> loggedMessages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
