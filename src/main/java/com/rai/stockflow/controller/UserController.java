package com.rai.stockflow.controller;

import com.rai.stockflow.dto.CreateUserRequest;
import com.rai.stockflow.dto.UserResponse;
import com.rai.stockflow.service.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Contratos iniciais de usuários, acessíveis somente a administradores. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRegistrationService registrationService;
    public UserController(UserRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(registrationService.register(request.email(), request.password(), request.role()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse findById(@PathVariable Long id) {
        return UserResponse.from(registrationService.findById(id));
    }
}
