package com.example.accountservice.controller;

import com.example.accountservice.dto.AccountResponse;
import com.example.accountservice.dto.RegisterAccountRequest;
import com.example.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterAccountRequest request) {
        AccountResponse response = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
