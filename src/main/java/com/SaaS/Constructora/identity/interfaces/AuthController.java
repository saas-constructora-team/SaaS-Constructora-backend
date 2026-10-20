package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.LoginUseCase;
import com.SaaS.Constructora.identity.application.RegisterTenantUseCase;
import com.SaaS.Constructora.identity.interfaces.dto.LoginRequest;
import com.SaaS.Constructora.identity.interfaces.dto.LoginResponse;
import com.SaaS.Constructora.identity.interfaces.dto.RegisterTenantRequest;
import com.SaaS.Constructora.identity.interfaces.dto.RegisterTenantResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterTenantUseCase registerTenantUseCase;

    AuthController(LoginUseCase loginUseCase, RegisterTenantUseCase registerTenantUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerTenantUseCase = registerTenantUseCase;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.login(request));
    }

    @PostMapping("/register")
    ResponseEntity<RegisterTenantResponse> register(@Valid @RequestBody RegisterTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerTenantUseCase.register(request));
    }
}
