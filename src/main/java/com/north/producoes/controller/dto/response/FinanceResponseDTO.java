package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.FinanceEntity;

public record FinanceResponseDTO(
        Long id,
        Long client,
        String description,
        Double value,
        String status,
        String expirationDate
) {
    public static FinanceResponseDTO from(FinanceEntity entity) {
        return new FinanceResponseDTO(
                entity.getId(),
                entity.getClient().getId(),
                entity.getDescription(),
                entity.getValue(),
                entity.getStatus().name(),
                entity.getExpirationDate().toString()
        );
    }
}
