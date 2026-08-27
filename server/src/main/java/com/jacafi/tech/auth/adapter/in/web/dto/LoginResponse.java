package com.jacafi.tech.auth.adapter.in.web.dto;

import com.jacafi.tech.auth.application.service.LoginResult;

public record LoginResponse(String username, String accessToken) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.username(), result.accessToken());
    }

    @Override
    public String toString() {
        return "LoginResponse[username=***, accessToken=***]";
    }
}
