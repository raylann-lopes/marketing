package com.north.producoes.controller.dto.response;

import com.north.producoes.integration.meta.dto.MetaAdsActionDTO;

public record MetaAdsActionResponseDTO(
        String type,
        Long value
) {
    public static MetaAdsActionResponseDTO from(MetaAdsActionDTO dto) {
        return new MetaAdsActionResponseDTO(
                dto.actionType(),
                longValue(dto.value())
        );
    }

    private static Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.valueOf(value);
    }
}
