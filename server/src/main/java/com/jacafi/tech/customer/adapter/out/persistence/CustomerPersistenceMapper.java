package com.jacafi.tech.customer.adapter.out.persistence;

import com.jacafi.tech.customer.domain.entity.Customer;
import com.jacafi.tech.customer.domain.entity.TaxId;

final class CustomerPersistenceMapper {

    private CustomerPersistenceMapper() {}

    static CustomerJpaEntity toJpa(Customer customer) {
        return new CustomerJpaEntity(
                customer.id(),
                customer.taxId().value(),
                customer.name(),
                customer.tradeName(),
                customer.email(),
                customer.phone(),
                customer.active());
    }

    static Customer toDomain(CustomerJpaEntity entity) {
        return Customer.restore(
                entity.id(),
                TaxId.of(entity.taxId()),
                entity.name(),
                entity.tradeName(),
                entity.email(),
                entity.phone(),
                entity.active(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
