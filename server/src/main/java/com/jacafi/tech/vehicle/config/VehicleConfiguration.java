package com.jacafi.tech.vehicle.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jacafi.tech.auth.application.port.CurrentAuthenticatedUserPort;
import com.jacafi.tech.shared.application.AuditTrailPort;
import com.jacafi.tech.vehicle.application.port.VehicleRepositoryPort;
import com.jacafi.tech.vehicle.application.service.FindVehicleService;
import com.jacafi.tech.vehicle.application.service.ListCurrentCustomerVehiclesService;
import com.jacafi.tech.vehicle.application.service.ListCustomerVehiclesService;
import com.jacafi.tech.vehicle.application.service.RegisterVehicleService;
import com.jacafi.tech.vehicle.application.service.RemoveVehicleService;
import com.jacafi.tech.vehicle.application.service.UpdateVehicleService;
import com.jacafi.tech.vehicle.application.service.VehicleAccessPolicy;

@Configuration
public class VehicleConfiguration {

    @Bean
    VehicleAccessPolicy vehicleAccessPolicy(CurrentAuthenticatedUserPort currentUser) {
        return new VehicleAccessPolicy(currentUser);
    }

    @Bean
    RegisterVehicleService registerVehicleService(
            VehicleRepositoryPort vehicles, AuditTrailPort auditTrail, VehicleAccessPolicy access, Clock clock) {
        return new RegisterVehicleService(vehicles, auditTrail, access, clock);
    }

    @Bean
    FindVehicleService findVehicleService(VehicleRepositoryPort vehicles, VehicleAccessPolicy access) {
        return new FindVehicleService(vehicles, access);
    }

    @Bean
    ListCustomerVehiclesService listCustomerVehiclesService(
            VehicleRepositoryPort vehicles, VehicleAccessPolicy access) {
        return new ListCustomerVehiclesService(vehicles, access);
    }

    @Bean
    ListCurrentCustomerVehiclesService listCurrentCustomerVehiclesService(
            VehicleRepositoryPort vehicles, VehicleAccessPolicy access) {
        return new ListCurrentCustomerVehiclesService(vehicles, access);
    }

    @Bean
    UpdateVehicleService updateVehicleService(
            VehicleRepositoryPort vehicles, AuditTrailPort auditTrail, VehicleAccessPolicy access, Clock clock) {
        return new UpdateVehicleService(vehicles, auditTrail, access, clock);
    }

    @Bean
    RemoveVehicleService removeVehicleService(
            VehicleRepositoryPort vehicles, AuditTrailPort auditTrail, VehicleAccessPolicy access, Clock clock) {
        return new RemoveVehicleService(vehicles, auditTrail, access, clock);
    }
}
