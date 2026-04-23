package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AccountConfigRequestDTO(
        @NotNull(message = "Cliente é obrigatório")
        @Positive(message = "ClientId deve ser maior que zero")
        Long clientId,
        @jakarta.validation.constraints.NotBlank(message = "igUserId é obrigatório")
        @Pattern(regexp = "\\d+", message = "igUserId deve conter apenas dígitos")
        @Size(max = 64, message = "igUserId deve ter no máximo 64 caracteres")
        String igUserId,
        @Size(max = 64, message = "instagramAccountId deve ter no máximo 64 caracteres")
        String instagramAccountId,
        @Size(max = 2048, message = "accessToken deve ter no máximo 2048 caracteres")
        String accessToken
) {}
