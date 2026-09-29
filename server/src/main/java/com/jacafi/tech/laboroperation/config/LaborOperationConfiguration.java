package com.jacafi.tech.laboroperation.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.laboroperation.application.port.LaborOperationRepository;
import com.jacafi.tech.laboroperation.application.service.DeactivateLaborOperationService;
import com.jacafi.tech.laboroperation.application.service.FindLaborOperationService;
import com.jacafi.tech.laboroperation.application.service.LaborOperationAccessPolicy;
import com.jacafi.tech.laboroperation.application.service.ListLaborOperationsService;
import com.jacafi.tech.laboroperation.application.service.RegisterLaborOperationService;
import com.jacafi.tech.laboroperation.application.service.UpdateLaborOperationService;
import com.jacafi.tech.shared.application.AuditTrailPort;

@Configuration
public class LaborOperationConfiguration {
    @Bean
    LaborOperationAccessPolicy laborOperationAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new LaborOperationAccessPolicy(currentUser);
    }

    @Bean
    RegisterLaborOperationService registerLaborOperationService(
            LaborOperationRepository operations,
            AuditTrailPort auditTrail,
            LaborOperationAccessPolicy access,
            Clock clock) {
        return new RegisterLaborOperationService(operations, auditTrail, access, clock);
    }

    @Bean
    FindLaborOperationService findLaborOperationService(
            LaborOperationRepository operations, LaborOperationAccessPolicy access) {
        return new FindLaborOperationService(operations, access);
    }

    @Bean
    ListLaborOperationsService listLaborOperationsService(
            LaborOperationRepository operations, LaborOperationAccessPolicy access) {
        return new ListLaborOperationsService(operations, access);
    }

    @Bean
    UpdateLaborOperationService updateLaborOperationService(
            LaborOperationRepository operations,
            AuditTrailPort auditTrail,
            LaborOperationAccessPolicy access,
            Clock clock) {
        return new UpdateLaborOperationService(operations, auditTrail, access, clock);
    }

    @Bean
    DeactivateLaborOperationService deactivateLaborOperationService(
            LaborOperationRepository operations,
            AuditTrailPort auditTrail,
            LaborOperationAccessPolicy access,
            Clock clock) {
        return new DeactivateLaborOperationService(operations, auditTrail, access, clock);
    }
}
