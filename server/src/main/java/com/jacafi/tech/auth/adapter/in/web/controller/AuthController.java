package com.jacafi.tech.auth.adapter.in.web.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.auth.adapter.in.web.api.AuthApi;
import com.jacafi.tech.auth.adapter.in.web.dto.LoginRequest;
import com.jacafi.tech.auth.adapter.in.web.dto.LoginResponse;
import com.jacafi.tech.auth.application.service.AuthenticateUserService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {

    private final AuthenticateUserService authenticateUserService;

    public AuthController(AuthenticateUserService authenticateUserService) {
        this.authenticateUserService = authenticateUserService;
    }

    @Override
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return LoginResponse.from(authenticateUserService.login(request.username(), request.password()));
    }
}
