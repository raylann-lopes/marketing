package com.north.producoes.integration.meta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaAdsAccountDTO(
        String id,
        @JsonProperty("account_id")
        String accountId,
        String name
) {
}
