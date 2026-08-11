package com.north.producoes.controller.dto.response;

import java.math.BigDecimal;

public record MetaAdsReportSummaryResponseDTO(
        BigDecimal spend,
        Long reach,
        Long impressions,
        Long clicks,
        BigDecimal ctr,
        BigDecimal cpc,
        BigDecimal cpm
) {
    public static MetaAdsReportSummaryResponseDTO from(MetaAdsAccountInsightResponseDTO dto) {
        return new MetaAdsReportSummaryResponseDTO(
                dto.spend(),
                dto.reach(),
                dto.impressions(),
                dto.clicks(),
                dto.ctr(),
                dto.cpc(),
                dto.cpm()
        );
    }
}
