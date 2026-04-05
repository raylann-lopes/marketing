package com.north.producoes.controller.dto.response;

public record LoginResponseDTO(String token, String refreshToken, String role, Long userId) {
    public LoginResponseDTO(String token, String refreshToken, String role) {
        this(token, refreshToken, role, null);
    }
}
