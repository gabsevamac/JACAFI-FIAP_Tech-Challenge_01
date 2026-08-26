package com.jacafi.tech.customer.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacafi.tech.customer.entity.Customer;
import com.jacafi.tech.customer.entity.TaxId;
import com.jacafi.tech.customer.exception.CustomerAlreadyExistsException;
import com.jacafi.tech.customer.exception.CustomerNotFoundException;
import com.jacafi.tech.customer.repository.CustomerRepository;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    /**
     * @param rawTaxId a CPF or a CNPJ, in any punctuation; which of the two it is comes from the
     *                 value itself, so the caller does not declare it
     */
    @Transactional
    public Customer create(String rawTaxId, String name, String tradeName, String email, String phone) {
        var taxId = TaxId.of(rawTaxId);

        if (repository.existsByTaxId(taxId)) {
            throw new CustomerAlreadyExistsException();
        }

        return repository.save(Customer.create(taxId, name, tradeName, email, phone));
    }

    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return repository
                .findById(Objects.requireNonNull(id, "Customer id must not be null"))
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Customer findByTaxId(String rawTaxId) {
        return repository.findByTaxId(TaxId.of(rawTaxId)).orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Page<Customer> list(Boolean active, Pageable pageable) {
        Objects.requireNonNull(pageable, "Pageable must not be null");
        return active == null ? repository.findAll(pageable) : repository.findAllByActive(active, pageable);
    }

    @Transactional
    public Customer update(UUID id, String name, String tradeName, String email, String phone) {
        var customer = findById(id);
        customer.updateName(name, tradeName);
        customer.updateContactInformation(email, phone);
        return customer;
    }

    @Transactional
    public void deactivate(UUID id) {
        findById(id).deactivate();
    }
}
