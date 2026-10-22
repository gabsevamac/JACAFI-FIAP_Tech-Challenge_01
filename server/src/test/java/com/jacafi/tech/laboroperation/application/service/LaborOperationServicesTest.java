package com.jacafi.tech.laboroperation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.laboroperation.domain.exception.DuplicateLaborOperationException;
import com.jacafi.tech.laboroperation.domain.exception.LaborOperationNotFoundException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;
import com.jacafi.tech.shared.domain.ErrorCode;
import com.jacafi.tech.shared.security.AccountAccessDeniedException;
import com.jacafi.tech.shared.security.AuthenticatedUser;
import com.jacafi.tech.shared.security.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.security.Role;

class LaborOperationServicesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void employeeRegistrationPersistsThenRecordsTheSharedAuditEvent() {
        Operations operations = new Operations();
        Trail trail = new Trail();

        LaborOperation operation = new RegisterLaborOperationService(operations, trail, employee(), CLOCK)
                .register("Oil change", "Replace engine oil.", new BigDecimal("89.90"));

        assertThat(operations.byId).containsKey(operation.id());
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("REGISTERED");
    }

    @Test
    void duplicateActiveNameIsRejectedBeforeWritingAnotherItem() {
        Operations operations = new Operations();
        operations.save(LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK));

        assertThatThrownBy(() -> new RegisterLaborOperationService(operations, new Trail(), employee(), CLOCK)
                        .register("  Oil change  ", null, new BigDecimal("99.90")))
                .isInstanceOf(DuplicateLaborOperationException.class);
        assertThat(operations.byId).hasSize(1);
    }

    @Test
    void customerCannotReadOrManageLaborOperations() {
        Operations operations = new Operations();
        LaborOperationAccessPolicy access = customer();

        assertThatThrownBy(() -> new RegisterLaborOperationService(operations, new Trail(), access, CLOCK)
                        .register("Oil change", null, new BigDecimal("89.90")))
                .isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(() -> new FindLaborOperationService(operations, access).findById(UUID.randomUUID()))
                .isInstanceOf(AccountAccessDeniedException.class);
    }

    @Test
    void employeeFindsAndListsActiveOperations() {
        Operations operations = new Operations();
        LaborOperation operation = operations.save(oilChange());

        assertThat(new FindLaborOperationService(operations, employee()).findById(operation.id()))
                .isSameAs(operation);
        assertThat(new ListLaborOperationsService(operations, employee())
                        .list(new PageQuery(0, 20, List.of(SortCriterion.ascending("id"))))
                        .content())
                .containsExactly(operation);
    }

    @Test
    void updatePersistsTheChangesThenRecordsTheSharedAuditEvent() {
        Operations operations = new Operations();
        Trail trail = new Trail();
        LaborOperation operation = operations.save(oilChange());

        LaborOperation updated = new UpdateLaborOperationService(operations, trail, employee(), CLOCK)
                .update(operation.id(), "Synthetic oil change", null, new BigDecimal("129.90"));

        assertThat(updated.name()).isEqualTo("Synthetic oil change");
        assertThat(updated.basePrice()).isEqualByComparingTo("129.90");
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("UPDATED");
    }

    @Test
    void updateRejectsANameUsedByAnotherActiveOperation() {
        Operations operations = new Operations();
        Trail trail = new Trail();
        operations.save(
                LaborOperation.register(UUID.randomUUID(), "Tire rotation", null, new BigDecimal("40.00"), CLOCK));
        LaborOperation operation = operations.save(oilChange());

        assertThatThrownBy(() -> new UpdateLaborOperationService(operations, trail, employee(), CLOCK)
                        .update(operation.id(), "tire rotation", null, new BigDecimal("40.00")))
                .isInstanceOf(DuplicateLaborOperationException.class);
        assertThat(trail.events).isEmpty();
    }

    @Test
    void deactivatedOperationIsNoLongerFoundAndTheAuditEventIsRecorded() {
        Operations operations = new Operations();
        Trail trail = new Trail();
        LaborOperation operation = operations.save(oilChange());

        new DeactivateLaborOperationService(operations, trail, employee(), CLOCK).deactivate(operation.id());

        assertThat(operations.findActiveById(operation.id())).isEmpty();
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("DEACTIVATED");
    }

    @Test
    void unknownOperationIsReportedAsNotFound() {
        assertThatThrownBy(
                        () -> new FindLaborOperationService(new Operations(), employee()).findById(UUID.randomUUID()))
                .isInstanceOfSatisfying(
                        LaborOperationNotFoundException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.LABOR_OPERATION_NOT_FOUND));
    }

    private static LaborOperation oilChange() {
        return LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK);
    }

    private static LaborOperationAccessPolicy employee() {
        return policy("employee", Set.of(Role.EMPLOYEE));
    }

    private static LaborOperationAccessPolicy customer() {
        return policy("customer", Set.of(Role.CUSTOMER));
    }

    private static LaborOperationAccessPolicy policy(String username, Set<Role> roles) {
        CurrentAuthenticatedUserPort user =
                () -> new AuthenticatedUser(UUID.randomUUID().toString(), username, roles, null);
        return new LaborOperationAccessPolicy(user);
    }

    private static final class Operations implements LaborOperationRepository {
        private final Map<UUID, LaborOperation> byId = new LinkedHashMap<>();

        @Override
        public LaborOperation save(LaborOperation operation) {
            byId.put(operation.id(), operation);
            return operation;
        }

        @Override
        public Optional<LaborOperation> findActiveById(UUID id) {
            return Optional.ofNullable(byId.get(id)).filter(LaborOperation::active);
        }

        @Override
        public PageResult<LaborOperation> findActive(PageQuery query) {
            List<LaborOperation> active =
                    byId.values().stream().filter(LaborOperation::active).toList();
            return PageResult.of(active, query.page(), query.size(), active.size());
        }

        @Override
        public boolean existsActiveWithName(String name) {
            return byId.values().stream()
                    .anyMatch(
                            operation -> operation.active() && operation.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsActiveWithNameExcluding(String name, UUID id) {
            return byId.values().stream()
                    .anyMatch(operation -> operation.active()
                            && !operation.id().equals(id)
                            && operation.name().equalsIgnoreCase(name));
        }
    }

    private static final class Trail implements AuditTrailPort {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public void record(AuditEvent event) {
            events.add(event);
        }
    }
}
