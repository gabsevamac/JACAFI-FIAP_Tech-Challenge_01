package com.jacafi.tech.features.client.create;

import com.jacafi.tech.features.client.domain.PersonType;

public record CreateClientCommand(
        PersonType personType,
        String taxIdentifier,
        String name,
        String tradeName,
        String email,
        String phone) {
}
