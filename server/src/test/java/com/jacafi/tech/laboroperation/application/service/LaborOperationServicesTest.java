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

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.domain.entity.LaborOperation;
import com.jacafi.tech.laboroperation.domain.exception.DuplicateLaborOperationException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

class LaborOperationServicesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void managerRegistrationPersistsThenRecordsTheSharedAuditEvent() {
        Operations operations = new Operations();
        Trail trail = new Trail();

        LaborOperation operation = new RegisterLaborOperationService(operations, trail, manager(), CLOCK)
                .register("Oil change", "Replace engine oil.", new BigDecimal("89.90"));

        assertThat(operations.byId).containsKey(operation.id());
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("REGISTERED");
    }

    @Test
    void duplicateActiveNameIsRejectedBeforeWritingAnotherItem() {
        Operations operations = new Operations();
        operations.save(LaborOperation.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK));

        assertThatThrownBy(() -> new RegisterLaborOperationService(operations, new Trail(), manager(), CLOCK)
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

    private static LaborOperationAccessPolicy manager() {
        return policy("manager", Set.of(Role.MANAGER));
    }

    private static LaborOperationAccessPolicy customer() {
        return policy("customer", Set.of(Role.CUSTOMER));
    }

    private static LaborOperationAccessPolicy policy(String username, Set<Role> roles) {
        CurrentAuthenticatedUserPort user = () -> new AuthenticatedUser(UUID.randomUUID(), username, roles, null);
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
