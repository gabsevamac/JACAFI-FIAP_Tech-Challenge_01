package com.jacafi.tech.customer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.customer.application.port.CustomerRepositoryPort;
import com.jacafi.tech.customer.application.service.CustomerAccessPolicy;
import com.jacafi.tech.customer.application.service.DeactivateCustomerService;
import com.jacafi.tech.customer.application.service.FindCustomerByTaxIdService;
import com.jacafi.tech.customer.application.service.FindCustomerService;
import com.jacafi.tech.customer.application.service.GetCurrentCustomerService;
import com.jacafi.tech.customer.application.service.ListCustomersService;
import com.jacafi.tech.customer.application.service.RegisterCustomerService;
import com.jacafi.tech.customer.application.service.UpdateCurrentCustomerService;
import com.jacafi.tech.customer.application.service.UpdateCustomerService;

@Configuration
public class CustomerConfiguration {

    @Bean
    CustomerAccessPolicy customerAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new CustomerAccessPolicy(currentUser);
    }

    @Bean
    RegisterCustomerService registerCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new RegisterCustomerService(customers, access);
    }

    @Bean
    FindCustomerService findCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new FindCustomerService(customers, access);
    }

    @Bean
    FindCustomerByTaxIdService findCustomerByTaxIdService(
            CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new FindCustomerByTaxIdService(customers, access);
    }

    @Bean
    ListCustomersService listCustomersService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new ListCustomersService(customers, access);
    }

    @Bean
    UpdateCustomerService updateCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new UpdateCustomerService(customers, access);
    }

    @Bean
    DeactivateCustomerService deactivateCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new DeactivateCustomerService(customers, access);
    }

    @Bean
    GetCurrentCustomerService getCurrentCustomerService(CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new GetCurrentCustomerService(customers, access);
    }

    @Bean
    UpdateCurrentCustomerService updateCurrentCustomerService(
            CustomerRepositoryPort customers, CustomerAccessPolicy access) {
        return new UpdateCurrentCustomerService(customers, access);
    }
}
