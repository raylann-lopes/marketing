package com.north.producoes.integration.meta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MetaAdsInsightDTO(
        @JsonProperty("account_id")
        String accountId,

        @JsonProperty("account_name")
        String accountName,

        String spend,
        String reach,
        String impressions,
        String clicks,
        String ctr,
        String cpc,
        String cpm,
        List<MetaAdsActionDTO> actions,

        @JsonProperty("date_start")
        String dateStart,

        @JsonProperty("date_stop")
        String dateStop
) {}
