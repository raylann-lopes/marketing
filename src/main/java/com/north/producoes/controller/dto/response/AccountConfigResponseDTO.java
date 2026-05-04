package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.AccountConfigEntity;

import java.time.LocalDateTime;

public record AccountConfigResponseDTO(
        Long id,
        Long clientId,
        String igUserId,
        String accessTokenMasked,
        String configuredBy,
        LocalDateTime configuredAt
) {
    public static AccountConfigResponseDTO from(AccountConfigEntity entity) {
        return new AccountConfigResponseDTO(
                entity.getId(),
                entity.getClient().getId(),
                entity.getIgUserId(),
                maskToken(entity.getAccessToken()),
                entity.getConfiguredBy(),
                entity.getConfiguredAt()
        );
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        if (token.length() <= 8) {
            return "********";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
