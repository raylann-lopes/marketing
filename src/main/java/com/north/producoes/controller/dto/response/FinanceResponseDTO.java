package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.FinanceEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinanceResponseDTO(
        Long id,
        Long clientId,
        String description,
        BigDecimal value,
        String status,
        LocalDateTime expirationDate,
        LocalDateTime paymentDate,
        String type
) {
    public static FinanceResponseDTO from(FinanceEntity entity) {
        return new FinanceResponseDTO(
                entity.getId(),
                entity.getClient().getId(),
                entity.getDescription(),
                entity.getValue(),
                entity.getStatus().name(),
                entity.getExpirationDate(),
                entity.getPaymentDate(),
                entity.getType() != null ? entity.getType().name() : null
        );
    }
}
