package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AccountConfigRequestDTO(
        @NotNull Long clientId,
        // ID numérico da conta Business no Facebook/Instagram Graph API
        @NotBlank @Pattern(regexp = "\\d+", message = "instagramAccountId deve conter apenas dígitos")
        String instagramAccountId
) {}
