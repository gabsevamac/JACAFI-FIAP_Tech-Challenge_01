package com.jacafi.tech.auth.application.service;

public record LoginResult(String username, String accessToken) {
    @Override
    public String toString() {
        return "LoginResult[username=***, accessToken=***]";
    }
}
