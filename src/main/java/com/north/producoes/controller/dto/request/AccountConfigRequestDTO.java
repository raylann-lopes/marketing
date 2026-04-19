package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AccountConfigRequestDTO(
        @NotNull(message = "Cliente é obrigatório")
        @Positive(message = "ClientId deve ser maior que zero")
        Long clientId,
        // ID numérico da conta Business no Facebook/Instagram Graph API
        @NotBlank(message = "Instagram Account ID é obrigatório")
        @Pattern(regexp = "\\d+", message = "instagramAccountId deve conter apenas dígitos")
        @Size(max = 64, message = "instagramAccountId deve ter no máximo 64 caracteres")
        String instagramAccountId
) {}
