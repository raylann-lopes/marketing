package com.north.producoes.controller.dto.request;

import java.time.LocalDate;

public record MetaAdsAccountInsightRequestDTO(
        Long clientId,
        LocalDate dateStart,
        LocalDate dateStop
) {
}
