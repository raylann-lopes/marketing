package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceRequestDTO(
        @NotNull(message = "Cliente é obrigatório")
        @Positive(message = "Cliente deve ser maior que zero")
        Long clientId,

        String description,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        FinanceStatusEnum status,

        FinanceTypeEnum type,

        @NotNull(message = "Data de vencimento é obrigatória")
        LocalDate expirationDate,

        LocalDate paymentDate
) {}
