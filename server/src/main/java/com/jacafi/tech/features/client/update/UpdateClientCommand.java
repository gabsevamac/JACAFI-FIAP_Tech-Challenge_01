package com.jacafi.tech.features.client.update;

public record UpdateClientCommand(
        String name,
        String tradeName,
        String email,
        String phone) {
}
