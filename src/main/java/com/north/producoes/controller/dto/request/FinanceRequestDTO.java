package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record FinanceRequestDTO(
        @NotNull(message = "Cliente é obrigatório")
        @Positive(message = "Cliente deve ser maior que zero")
        Long client,
        String description,
        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        Double value,
        @NotBlank(message = "Status é obrigatório")
        @Pattern(regexp = "PENDING|PAY", message = "Status deve ser PENDING ou PAY")
        String status,
        @NotBlank(message = "Data de vencimento é obrigatória")
        String expirationDate
) {}
