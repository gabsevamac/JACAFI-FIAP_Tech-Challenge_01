package com.jacafi.tech.auth.adapter.in.web.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jacafi.tech.auth.adapter.in.web.api.UserAccountApi;
import com.jacafi.tech.auth.adapter.in.web.dto.CreateUserAccountRequest;
import com.jacafi.tech.auth.adapter.in.web.dto.UserAccountResponse;
import com.jacafi.tech.auth.application.service.CreateUserAccountService;
import com.jacafi.tech.auth.application.service.DeactivateUserAccountService;
import com.jacafi.tech.auth.application.service.FindUserAccountService;
import com.jacafi.tech.auth.application.service.GetCurrentUserAccountService;
import com.jacafi.tech.auth.application.service.ListUserAccountsService;
import com.jacafi.tech.auth.domain.entity.UserAccount;

@RestController
@RequestMapping("/api/v1/user-accounts")
public class UserAccountController implements UserAccountApi {

    private final CreateUserAccountService createUserAccount;
    private final ListUserAccountsService listUserAccounts;
    private final FindUserAccountService findUserAccount;
    private final GetCurrentUserAccountService getCurrentUserAccount;
    private final DeactivateUserAccountService deactivateUserAccount;

    public UserAccountController(
            CreateUserAccountService createUserAccount,
            ListUserAccountsService listUserAccounts,
            FindUserAccountService findUserAccount,
            GetCurrentUserAccountService getCurrentUserAccount,
            DeactivateUserAccountService deactivateUserAccount) {
        this.createUserAccount = createUserAccount;
        this.listUserAccounts = listUserAccounts;
        this.findUserAccount = findUserAccount;
        this.getCurrentUserAccount = getCurrentUserAccount;
        this.deactivateUserAccount = deactivateUserAccount;
    }

    @Override
    @PostMapping
    public ResponseEntity<UserAccountResponse> create(@Valid @RequestBody CreateUserAccountRequest request) {
        UserAccount account =
                createUserAccount.create(request.username(), request.password(), request.roles(), request.customerId());
        return ResponseEntity.created(URI.create("/api/v1/user-accounts/" + account.id()))
                .body(UserAccountResponse.from(account));
    }

    @Override
    @GetMapping
    public List<UserAccountResponse> list() {
        return listUserAccounts.list().stream().map(UserAccountResponse::from).toList();
    }

    @Override
    @GetMapping("/{id}")
    public UserAccountResponse get(@PathVariable UUID id) {
        return UserAccountResponse.from(findUserAccount.find(id));
    }

    @Override
    @GetMapping("/me")
    public UserAccountResponse me() {
        return UserAccountResponse.from(getCurrentUserAccount.get());
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateUserAccount.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
