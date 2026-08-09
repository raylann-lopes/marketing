package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.AccountConfigEntity;

import java.time.LocalDateTime;

/**
 * Resposta da configuração de conta Instagram.
 *
 * igUserId e accessToken completo NÃO são expostos —
 * apenas uma máscara do token é retornada para confirmar
 * que a configuração existe, sem vazar o valor real.
 */
public record AccountConfigResponseDTO(
        Long id,
        Long clientId,
        String metaAdAccountId,
        String accessTokenMasked,
        String configuredBy,
        LocalDateTime configuredAt
) {
    public static AccountConfigResponseDTO from(AccountConfigEntity entity) {
        return new AccountConfigResponseDTO(
                entity.getId(),
                entity.getClient().getId(),
                entity.getMetaAdAccountId(),
                maskToken(entity.getAccessToken()),
                entity.getConfiguredBy(),
                entity.getConfiguredAt()
        );
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "não configurado";
        if (token.length() <= 8) return "********";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
