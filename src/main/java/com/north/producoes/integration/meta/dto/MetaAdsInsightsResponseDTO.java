package com.north.producoes.integration.meta.dto;

import java.util.List;

public record MetaAdsInsightsResponseDTO(
        List<MetaAdsInsightDTO> data
) {}