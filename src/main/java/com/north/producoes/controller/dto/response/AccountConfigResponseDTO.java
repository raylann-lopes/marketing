package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.AccountConfigEntity;

import java.time.LocalDateTime;

public record AccountConfigResponseDTO(
        Long id,
        Long clientId,
        String instagramAccountId,
        String configuredBy,
        LocalDateTime configuredAt
) {
    public static AccountConfigResponseDTO from(AccountConfigEntity entity) {
        return new AccountConfigResponseDTO(
                entity.getId(),
                entity.getClient().getId(),
                entity.getInstagramAccountId(),
                entity.getConfiguredBy(),
                entity.getConfiguredAt()
        );
    }
}
