package com.jacafi.tech.vehicle.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.jacafi.tech.shared.application.PageQuery;
import com.jacafi.tech.shared.application.PageResult;
import com.jacafi.tech.vehicle.domain.LicensePlate;
import com.jacafi.tech.vehicle.domain.Vehicle;
import com.jacafi.tech.vehicle.domain.VehicleRepository;

/**
 * Hand-written stand-in for the repository, implementing the same contract in a map.
 *
 * <p>Written rather than mocked on purpose. A mock would let a test assert which methods were
 * called; this asserts what the use case actually achieved — and, because it honours the "active
 * only" rule of the port, it is what makes the plate-released-after-removal test meaningful
 * instead of a stubbed answer.
 *
 * <p>{@link #saveCount()} exists because the map stores aggregates by reference: a use case that
 * mutated an aggregate and forgot to save it would otherwise still appear to work.
 */
class InMemoryVehicleRepository implements VehicleRepository, VehicleQueries {

    private final Map<UUID, Vehicle> vehicles = new LinkedHashMap<>();
    private int saveCount;

    @Override
    public void save(Vehicle vehicle) {
        vehicles.put(vehicle.getId(), vehicle);
        saveCount++;
    }

    @Override
    public Optional<Vehicle> findActiveById(UUID id) {
        return Optional.ofNullable(vehicles.get(id)).filter(vehicle -> !vehicle.isRemoved());
    }

    @Override
    public Optional<Vehicle> findActiveByLicensePlate(LicensePlate licensePlate) {
        return activeVehicles()
                .filter(vehicle ->
                        vehicle.getLicensePlate().filter(licensePlate::equals).isPresent())
                .findFirst();
    }

    @Override
    public boolean existsActiveWithLicensePlate(LicensePlate licensePlate) {
        return findActiveByLicensePlate(licensePlate).isPresent();
    }

    @Override
    public PageResult<Vehicle> findActiveByCustomer(UUID customerId, PageQuery query) {
        List<Vehicle> matching = activeVehicles()
                .filter(vehicle -> vehicle.getCustomerId().equals(customerId))
                // Ordena por registro e desempata por identificador, imitando o criterio que a
                // lista branca impoe no adaptador real. Sem o desempate, dois veiculos registrados
                // no mesmo instante — o que o relogio fixo dos testes torna a regra, nao a excecao
                // — cairiam em ordem arbitraria e a paginacao ficaria instavel aqui tambem.
                .sorted(Comparator.comparing(Vehicle::getRegisteredAt).thenComparing(Vehicle::getId))
                .toList();

        int from = Math.min(query.page() * query.size(), matching.size());
        int to = Math.min(from + query.size(), matching.size());
        return PageResult.of(new ArrayList<>(matching.subList(from, to)), query.page(), query.size(), matching.size());
    }

    int saveCount() {
        return saveCount;
    }

    private java.util.stream.Stream<Vehicle> activeVehicles() {
        return vehicles.values().stream().filter(vehicle -> !vehicle.isRemoved());
    }
}
