package com.north.producoes.entity.dto.response;

import com.north.producoes.entity.FinanceEntity;

public record FinanceResponse(
        Long id,
        Long client,
        String description,
        Double value,
        String status,
        String expirationDate
) {
    public static FinanceResponse from(FinanceEntity entity) {
        return new FinanceResponse(
                entity.getId(),
                entity.getClient().getId(),
                entity.getDescription(),
                entity.getValue(),
                entity.getStatus().name(),
                entity.getExpirationDate().toString()
        );
    }
}
