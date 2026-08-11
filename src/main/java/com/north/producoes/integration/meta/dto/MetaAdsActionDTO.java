package com.north.producoes.integration.meta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetaAdsActionDTO(
        @JsonProperty("action_type")
        String actionType,
        String value
){
}
