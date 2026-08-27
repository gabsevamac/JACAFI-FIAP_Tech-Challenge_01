package com.jacafi.tech.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import com.jacafi.tech.customer.entity.Cpf;
import com.jacafi.tech.customer.entity.Customer;
import com.jacafi.tech.customer.entity.TaxId;
import com.jacafi.tech.customer.exception.CustomerAlreadyExistsException;
import com.jacafi.tech.customer.exception.CustomerNotFoundException;
import com.jacafi.tech.customer.repository.CustomerRepository;
import com.jacafi.tech.shared.adapter.out.persistence.SpringDataPaging;
import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.SortCriterion;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerService service;

    @Test
    void createsACustomerWithANormalizedTaxId() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var customer = service.create("529.982.247-25", "Maria", null, "maria@example.com", "11999999999");

        assertThat(customer.getTaxId().value()).isEqualTo("52998224725");
        assertThat(customer.isActive()).isTrue();
        verify(repository).save(customer);
    }

    @Test
    void rejectsAnExistingTaxId() {
        when(repository.existsByTaxId(new Cpf("52998224725"))).thenReturn(true);

        assertThatThrownBy(() -> service.create("52998224725", "Maria", null, "maria@example.com", "11999999999"))
                .isInstanceOf(CustomerAlreadyExistsException.class);
    }

    @Test
    void findsACustomerById() {
        var id = UUID.randomUUID();
        var customer = customer();
        when(repository.findById(id)).thenReturn(Optional.of(customer));

        assertThat(service.findById(id)).isSameAs(customer);
    }

    @Test
    void findsACustomerByNormalizedTaxId() {
        var customer = customer();
        when(repository.findByTaxId(new Cpf("52998224725"))).thenReturn(Optional.of(customer));

        assertThat(service.findByTaxId("529.982.247-25")).isSameAs(customer);
    }

    @Test
    void reportsAMissingCustomer() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void listsAllCustomersOrFiltersByActiveState() {
        // O service recebe PageQuery e converte para Pageable internamente, entao o stub precisa
        // casar com o Pageable que essa conversao produz — incluindo a ordenacao, que a lista
        // branca sempre acrescenta.
        var query = new PageQuery(0, 20, List.of(SortCriterion.ascending("id")));
        var pageable = SpringDataPaging.toPageable(query);
        when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));
        when(repository.findAllByActive(true, pageable)).thenReturn(Page.empty(pageable));

        assertThat(service.list(null, query).content()).isEmpty();
        assertThat(service.list(true, query).content()).isEmpty();
    }

    @Test
    void updatesMutableCustomerData() {
        var id = UUID.randomUUID();
        var customer = customer();
        when(repository.findById(id)).thenReturn(Optional.of(customer));

        var updated = service.update(id, "Maria da Silva", null, "novo@example.com", "11888888888");

        assertThat(updated.getName()).isEqualTo("Maria da Silva");
        assertThat(updated.getEmail()).isEqualTo("novo@example.com");
        assertThat(updated.getTaxId().value()).isEqualTo("52998224725");
    }

    @Test
    void rejectsUpdatingAMissingCustomer() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, "Maria", null, "maria@example.com", "11999999999"))
                .isInstanceOf(CustomerNotFoundException.class)
                // A mensagem vem do catalogo ErrorCode, em pt-BR, porque e o texto que o cliente
                // recebe. O teste afirma sobre o codigo, que e o contrato estavel.
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(CustomerNotFoundException.class))
                .extracting(e -> e.errorCode().code())
                .isEqualTo("CLI-001");
    }

    @Test
    void deactivatesTheCustomerKeepingItsData() {
        var id = UUID.randomUUID();
        var customer = customer();
        when(repository.findById(id)).thenReturn(Optional.of(customer));

        service.deactivate(id);

        assertThat(customer.isActive()).isFalse();
        // Deactivation is not deletion: the fiscal identity and the name survive.
        assertThat(customer.getTaxId()).isNotNull();
        assertThat(customer.getName()).isEqualTo("Maria");
    }

    private Customer customer() {
        return Customer.create(TaxId.of("52998224725"), "Maria", null, "maria@example.com", "11999999999");
    }
}
