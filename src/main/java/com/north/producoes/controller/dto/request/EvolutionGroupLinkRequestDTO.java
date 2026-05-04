package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvolutionGroupLinkRequestDTO(
        @NotNull(message = "ID do cliente é obrigatório")
        Long clientId,

        @NotBlank(message = "groupId é obrigatório")
        String groupId
) {}
