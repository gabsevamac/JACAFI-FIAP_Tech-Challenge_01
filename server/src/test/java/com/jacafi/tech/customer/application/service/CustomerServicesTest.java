package com.jacafi.tech.customer.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jacafi.tech.auth.application.port.AuthenticatedUser;
import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.auth.domain.entity.Role;
import com.jacafi.tech.auth.domain.exception.AccountAccessDeniedException;
import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.customer.domain.exception.CustomerAlreadyExistsException;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.shared.application.SortCriterion;

@ExtendWith(MockitoExtension.class)
class CustomerServicesTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Mock
    private CustomerRepositoryPort customers;

    @Mock
    private CurrentAuthenticatedUserPort currentUser;

    private CustomerAccessPolicy access;

    @BeforeEach
    void setUp() {
        access = new CustomerAccessPolicy(currentUser);
    }

    @Test
    void operationalUserRegistersCustomerAndPreventsDuplicateTaxId() {
        operationalUser();
        when(customers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer customer = new RegisterCustomerService(customers, access)
                .register("529.982.247-25", "Maria", null, "maria@example.com", "11999999999");

        assertThat(customer.taxId().value()).isEqualTo("52998224725");
        verify(customers).save(customer);

        when(customers.existsByTaxId(TaxId.of("52998224725"))).thenReturn(true);
        assertThatThrownBy(() -> new RegisterCustomerService(customers, access)
                        .register("52998224725", "Maria", null, "maria@example.com", "11999999999"))
                .isInstanceOf(CustomerAlreadyExistsException.class);
    }

    @Test
    void operationalUserCanFindLookupListUpdateAndDeactivateCustomers() {
        operationalUser();
        Customer customer = customer(CUSTOMER_ID);
        when(customers.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customers.findByTaxId(TaxId.of("52998224725"))).thenReturn(Optional.of(customer));
        PageQuery query = new PageQuery(0, 20, List.of(SortCriterion.ascending("id")));
        when(customers.findAll(null, query)).thenReturn(PageResult.of(List.of(customer), 0, 20, 1));
        when(customers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(new FindCustomerService(customers, access).find(CUSTOMER_ID)).isSameAs(customer);
        assertThat(new FindCustomerByTaxIdService(customers, access).find("529.982.247-25"))
                .isSameAs(customer);
        assertThat(new ListCustomersService(customers, access).list(null, query).content())
                .containsExactly(customer);

        assertThat(new UpdateCustomerService(customers, access)
                        .update(CUSTOMER_ID, "Maria da Silva", null, "novo@example.com", "11888888888"))
                .isSameAs(customer);
        new DeactivateCustomerService(customers, access).deactivate(CUSTOMER_ID);

        assertThat(customer.name()).isEqualTo("Maria da Silva");
        assertThat(customer.active()).isFalse();
    }

    @Test
    void customerIdentityIsResolvedFromTheAuthenticatedPrincipalOnly() {
        customerUser(CUSTOMER_ID);
        Customer customer = customer(CUSTOMER_ID);
        when(customers.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(customers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(new GetCurrentCustomerService(customers, access).get()).isSameAs(customer);
        assertThat(new UpdateCurrentCustomerService(customers, access)
                        .update("Maria da Silva", null, "novo@example.com", "11888888888"))
                .isSameAs(customer);

        assertThat(customer.name()).isEqualTo("Maria da Silva");
    }

    @Test
    void customerCannotUseAnIdBasedLookupBeforeTheRepositoryIsCalled() {
        customerUser(CUSTOMER_ID);

        assertThatThrownBy(() -> new FindCustomerService(customers, access).find(UUID.randomUUID()))
                .isInstanceOf(AccountAccessDeniedException.class);

        verifyNoInteractions(customers);
    }

    private void operationalUser() {
        when(currentUser.currentUser())
                .thenReturn(new AuthenticatedUser(UUID.randomUUID(), Set.of(Role.SERVICE_ADVISOR), null));
    }

    private void customerUser(UUID customerId) {
        when(currentUser.currentUser())
                .thenReturn(new AuthenticatedUser(UUID.randomUUID(), Set.of(Role.CUSTOMER), customerId));
    }

    private static Customer customer(UUID id) {
        return Customer.restore(
                id, TaxId.of("52998224725"), "Maria", null, "maria@example.com", "11999999999", true, 0, null, null);
    }
}
