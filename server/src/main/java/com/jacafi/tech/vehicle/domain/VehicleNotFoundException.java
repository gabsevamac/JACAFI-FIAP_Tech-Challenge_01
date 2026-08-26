package com.jacafi.tech.vehicle.domain;

import java.util.UUID;

import com.jacafi.tech.shared.web.BusinessException;
import com.jacafi.tech.shared.web.ErrorCode;

/**
 * No active vehicle exists for the given identifier.
 *
 * <p>A removed vehicle is indistinguishable from a non-existent one to any caller: its record
 * survives for the service history required by Art. 16 I, but it answers no query.
 */
public class VehicleNotFoundException extends BusinessException {

    public VehicleNotFoundException(UUID vehicleId) {
        // O identificador vai para o log, nao para a resposta. E chave substituta e nao dado
        // pessoal, mas devolve-lo confirmaria ao chamador quais identificadores existem, e a
        // resposta ja diz o que ele precisa saber: nao ha veiculo ativo com esse identificador.
        super(ErrorCode.VEHICLE_NOT_FOUND, "vehicleId=" + vehicleId);
    }

    /** For the lookup by plate, where the searched value may not be echoed back. */
    public VehicleNotFoundException() {
        super(ErrorCode.VEHICLE_NOT_FOUND);
    }
}
