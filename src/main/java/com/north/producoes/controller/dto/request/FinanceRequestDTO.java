package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FinanceRequestDTO(
        @NotBlank Long client,
        String description,
        @NotBlank Double value,
        @NotBlank String status,
        @NotBlank String expirationDate
) {}
