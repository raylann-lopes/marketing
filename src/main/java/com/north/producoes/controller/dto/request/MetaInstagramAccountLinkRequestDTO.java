package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MetaInstagramAccountLinkRequestDTO(
        @NotNull(message = "Cliente é obrigatório")
        @Positive(message = "ClientId deve ser maior que zero")
        Long clientId,

        @NotBlank(message = "pageId é obrigatório")
        @Pattern(regexp = "\\d+", message = "pageId deve conter apenas dígitos")
        @Size(max = 64, message = "pageId deve ter no máximo 64 caracteres")
        String pageId,

        @NotBlank(message = "igUserId é obrigatório")
        @Pattern(regexp = "\\d+", message = "igUserId deve conter apenas dígitos")
        @Size(max = 64, message = "igUserId deve ter no máximo 64 caracteres")
        String igUserId
) {}
