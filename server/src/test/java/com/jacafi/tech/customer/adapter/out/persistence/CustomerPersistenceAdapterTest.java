package com.jacafi.tech.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;
import com.jacafi.tech.customer.domain.exception.CustomerUpdateConflictException;

@ExtendWith(MockitoExtension.class)
class CustomerPersistenceAdapterTest {

    @Mock
    private CustomerJpaRepository repository;

    @Test
    void updatesAnEntityWhenTheAggregateCarriesItsCurrentVersion() {
        UUID id = UUID.randomUUID();
        CustomerJpaEntity existing = existingCustomer(id, 1);
        Customer changed = Customer.restore(
                id,
                TaxId.of("52998224725"),
                "Maria da Silva",
                null,
                "nova@example.com",
                "11888888888",
                true,
                1,
                null,
                null);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer saved = new CustomerPersistenceAdapter(repository).save(changed);

        verify(repository).save(same(existing));
        assertThat(existing.name()).isEqualTo("Maria da Silva");
        assertThat(existing.email()).isEqualTo("nova@example.com");
        assertThat(saved.id()).isEqualTo(id);
        assertThat(saved.version()).isOne();
    }

    @Test
    void rejectsAStaleAggregateBeforeApplyingOrPersistingIt() {
        UUID id = UUID.randomUUID();
        CustomerJpaEntity existing = existingCustomer(id, 1);
        Customer stale = Customer.restore(
                id,
                TaxId.of("52998224725"),
                "Maria da Silva",
                null,
                "nova@example.com",
                "11888888888",
                true,
                0,
                null,
                null);
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> new CustomerPersistenceAdapter(repository).save(stale))
                .isInstanceOf(CustomerUpdateConflictException.class);

        assertThat(existing.name()).isEqualTo("Maria");
        assertThat(existing.email()).isEqualTo("maria@example.com");
        verify(repository, never()).save(any());
    }

    private static CustomerJpaEntity existingCustomer(UUID id, long version) {
        CustomerJpaEntity customer =
                new CustomerJpaEntity(id, "52998224725", "Maria", null, "maria@example.com", "11999999999", true);
        ReflectionTestUtils.setField(customer, "version", version);
        return customer;
    }
}
