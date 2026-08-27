package com.jacafi.tech.customer.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;

@ExtendWith(MockitoExtension.class)
class CustomerPersistenceAdapterTest {

    @Mock
    private CustomerJpaRepository repository;

    @Test
    void updatesTheExistingJpaEntityInsteadOfReplacingItsAuditAndVersionState() {
        UUID id = UUID.randomUUID();
        CustomerJpaEntity existing =
                new CustomerJpaEntity(id, "52998224725", "Maria", null, "maria@example.com", "11999999999", true);
        Customer changed = Customer.restore(
                id,
                TaxId.of("52998224725"),
                "Maria da Silva",
                null,
                "nova@example.com",
                "11888888888",
                true,
                null,
                null);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Customer saved = new CustomerPersistenceAdapter(repository).save(changed);

        verify(repository).save(same(existing));
        assertThat(existing.name()).isEqualTo("Maria da Silva");
        assertThat(existing.email()).isEqualTo("nova@example.com");
        assertThat(saved.id()).isEqualTo(id);
    }
}
