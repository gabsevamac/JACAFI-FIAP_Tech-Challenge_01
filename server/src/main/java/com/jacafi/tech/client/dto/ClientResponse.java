package com.jacafi.tech.client.dto;

import com.jacafi.tech.client.entity.Client;
import com.jacafi.tech.client.entity.PersonType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        PersonType personType,
        String taxIdentifier,
        String name,
        String tradeName,
        String email,
        String phone,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ClientResponse from(Client client) {
        var party = client.getParty();
        return new ClientResponse(
                client.getId(),
                party.getPersonType(),
                party.getTaxIdentifier().getValue(),
                party.getName(),
                party.getTradeName(),
                client.getEmail(),
                client.getPhone(),
                client.isActive(),
                client.getCreatedAt(),
                client.getUpdatedAt());
    }
}
