package com.north.producoes.controller.api;

import com.north.producoes.entity.dto.request.LoginRequest;
import com.north.producoes.entity.dto.response.LoginResponse;
import com.north.producoes.security.refreshToken.RefreshRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Autenticação", description = "Endpoints de autenticação e renovação de token")
@RequestMapping("/api/auth")
public interface AuthApi {

    @Operation(summary = "Realiza login e retorna tokens de acesso")
    @ApiResponse(responseCode = "200", description = "Login realizado com sucesso")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest);

    @Operation(summary = "Renova o access token usando o refresh token")
    @ApiResponse(responseCode = "200", description = "Token renovado com sucesso")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    @PostMapping("/refresh")
    ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request);
}
