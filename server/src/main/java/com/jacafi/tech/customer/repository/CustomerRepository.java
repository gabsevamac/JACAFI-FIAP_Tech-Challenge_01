package com.jacafi.tech.customer.repository;

import com.jacafi.tech.customer.entity.Customer;
import com.jacafi.tech.customer.entity.TaxId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Storage of customers.
 *
 * <p>The nested traversals through {@code party} are gone along with the entity: the registration
 * now sits on the customer row, so a lookup by it is a plain derived query. The
 * {@code TaxIdConverter} turns the parameter into its column value.
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByTaxId(TaxId taxId);

    Optional<Customer> findByTaxId(TaxId taxId);

    Page<Customer> findAllByActive(boolean active, Pageable pageable);
}
