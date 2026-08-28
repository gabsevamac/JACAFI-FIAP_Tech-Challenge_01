package com.jacafi.tech.servicecatalog.application.service;

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
import com.jacafi.tech.servicecatalog.application.port.ServiceCatalogRepositoryPort;
import com.jacafi.tech.servicecatalog.domain.entity.ServiceCatalogItem;
import com.jacafi.tech.servicecatalog.domain.exception.DuplicateServiceCatalogItemException;
import com.jacafi.tech.shared.application.AuditEvent;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;

class ServiceCatalogServicesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void managerRegistrationPersistsThenRecordsTheSharedAuditEvent() {
        Items items = new Items();
        Trail trail = new Trail();

        ServiceCatalogItem item = new RegisterServiceCatalogItemService(items, trail, manager(), CLOCK)
                .register("Oil change", "Replace engine oil.", new BigDecimal("89.90"));

        assertThat(items.byId).containsKey(item.id());
        assertThat(trail.events).singleElement().extracting(AuditEvent::action).isEqualTo("REGISTERED");
    }

    @Test
    void duplicateActiveNameIsRejectedBeforeWritingAnotherItem() {
        Items items = new Items();
        items.save(ServiceCatalogItem.register(UUID.randomUUID(), "Oil change", null, new BigDecimal("89.90"), CLOCK));

        assertThatThrownBy(() -> new RegisterServiceCatalogItemService(items, new Trail(), manager(), CLOCK)
                        .register("  Oil change  ", null, new BigDecimal("99.90")))
                .isInstanceOf(DuplicateServiceCatalogItemException.class);
        assertThat(items.byId).hasSize(1);
    }

    @Test
    void customerCannotReadOrManageTheServiceCatalog() {
        Items items = new Items();
        ServiceCatalogAccessPolicy access = customer();

        assertThatThrownBy(() -> new RegisterServiceCatalogItemService(items, new Trail(), access, CLOCK)
                        .register("Oil change", null, new BigDecimal("89.90")))
                .isInstanceOf(AccountAccessDeniedException.class);
        assertThatThrownBy(() -> new FindServiceCatalogItemService(items, access).findById(UUID.randomUUID()))
                .isInstanceOf(AccountAccessDeniedException.class);
    }

    private static ServiceCatalogAccessPolicy manager() {
        return policy("manager", Set.of(Role.MANAGER));
    }

    private static ServiceCatalogAccessPolicy customer() {
        return policy("customer", Set.of(Role.CUSTOMER));
    }

    private static ServiceCatalogAccessPolicy policy(String username, Set<Role> roles) {
        CurrentAuthenticatedUserPort user = () -> new AuthenticatedUser(UUID.randomUUID(), username, roles, null);
        return new ServiceCatalogAccessPolicy(user);
    }

    private static final class Items implements ServiceCatalogRepositoryPort {
        private final Map<UUID, ServiceCatalogItem> byId = new LinkedHashMap<>();

        @Override
        public ServiceCatalogItem save(ServiceCatalogItem item) {
            byId.put(item.id(), item);
            return item;
        }

        @Override
        public Optional<ServiceCatalogItem> findActiveById(UUID id) {
            return Optional.ofNullable(byId.get(id)).filter(ServiceCatalogItem::active);
        }

        @Override
        public PageResult<ServiceCatalogItem> findActive(PageQuery query) {
            List<ServiceCatalogItem> active =
                    byId.values().stream().filter(ServiceCatalogItem::active).toList();
            return PageResult.of(active, query.page(), query.size(), active.size());
        }

        @Override
        public boolean existsActiveWithName(String name) {
            return byId.values().stream()
                    .anyMatch(item -> item.active() && item.name().equalsIgnoreCase(name));
        }

        @Override
        public boolean existsActiveWithNameExcluding(String name, UUID id) {
            return byId.values().stream()
                    .anyMatch(item -> item.active()
                            && !item.id().equals(id)
                            && item.name().equalsIgnoreCase(name));
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
