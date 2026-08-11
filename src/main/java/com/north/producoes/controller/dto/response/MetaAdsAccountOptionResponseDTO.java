package com.north.producoes.controller.dto.response;

import com.north.producoes.integration.meta.dto.MetaAdsAccountDTO;

public record MetaAdsAccountOptionResponseDTO(
        String id,
        String accountId,
        String name
) {
    public static MetaAdsAccountOptionResponseDTO from(MetaAdsAccountDTO dto) {
        return new MetaAdsAccountOptionResponseDTO(
        dto.id(),
        dto.accountId(),
        dto.name()
        );
    }
}
