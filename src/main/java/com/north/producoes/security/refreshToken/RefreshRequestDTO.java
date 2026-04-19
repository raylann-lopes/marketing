package com.north.producoes.security.refreshToken;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token obrigatório")
        String refreshToken) {}
