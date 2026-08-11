package com.north.producoes.controller.dto.response;

import com.north.producoes.integration.meta.dto.MetaAdsActionDTO;
import com.north.producoes.integration.meta.dto.MetaAdsInsightDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record MetaAdsAccountInsightResponseDTO(
        String accountId,
        String accountName,
        BigDecimal spend,
        Long reach,
        Long impressions,
        Long clicks,
        BigDecimal ctr,
        BigDecimal cpc,
        BigDecimal cpm,
        List<MetaAdsActionResponseDTO> actions,
        LocalDate dateStart,
        LocalDate dateStop
) {
    public static MetaAdsAccountInsightResponseDTO from(MetaAdsInsightDTO dto){
        return new MetaAdsAccountInsightResponseDTO(
                dto.accountId(),
                dto.accountName(),
                decimalValue(dto.spend()),
                longValue(dto.reach()),
                longValue(dto.impressions()),
                longValue(dto.clicks()),
                decimalValue(dto.ctr()),
                decimalValue(dto.cpc()),
                decimalValue(dto.cpm()),
                (dto.actions() == null ? Collections.<MetaAdsActionDTO>emptyList() : dto.actions()).stream()
                        .map(MetaAdsActionResponseDTO::from)
                        .toList(),
                dateValue(dto.dateStart()),
                dateValue(dto.dateStop())
        );
    }

    private static BigDecimal decimalValue(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private static Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.valueOf(value);
    }

    private static LocalDate dateValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value);
    }
}
