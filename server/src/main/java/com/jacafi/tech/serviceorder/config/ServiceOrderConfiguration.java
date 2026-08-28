package com.jacafi.tech.serviceorder.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.serviceorder.application.port.ServiceOrderRepositoryPort;
import com.jacafi.tech.serviceorder.application.service.CompleteServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.DecideEstimateService;
import com.jacafi.tech.serviceorder.application.service.DeliverServiceOrderService;
import com.jacafi.tech.serviceorder.application.service.GenerateServiceOrderEstimateService;
import com.jacafi.tech.serviceorder.application.service.ServiceOrderAccessPolicy;
import com.jacafi.tech.serviceorder.application.service.StartServiceOrderDiagnosisService;
import com.jacafi.tech.shared.application.AuditTrailPort;

@Configuration
public class ServiceOrderConfiguration {
    @Bean
    ServiceOrderAccessPolicy serviceOrderAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new ServiceOrderAccessPolicy(currentUser);
    }

    @Bean
    StartServiceOrderDiagnosisService startServiceOrderDiagnosisService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new StartServiceOrderDiagnosisService(orders, auditTrail, access, clock);
    }

    @Bean
    GenerateServiceOrderEstimateService generateServiceOrderEstimateService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new GenerateServiceOrderEstimateService(orders, auditTrail, access, clock);
    }

    @Bean
    DecideEstimateService decideEstimateService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new DecideEstimateService(orders, auditTrail, access, clock);
    }

    @Bean
    CompleteServiceOrderService completeServiceOrderService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new CompleteServiceOrderService(orders, auditTrail, access, clock);
    }

    @Bean
    DeliverServiceOrderService deliverServiceOrderService(
            ServiceOrderRepositoryPort orders,
            AuditTrailPort auditTrail,
            ServiceOrderAccessPolicy access,
            Clock clock) {
        return new DeliverServiceOrderService(orders, auditTrail, access, clock);
    }
}
