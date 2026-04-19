package com.north.producoes.security.refreshToken;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
        @NotBlank(message = "Refresh token obrigatório")
        String refreshToken) {}
