package com.north.producoes.integration.meta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.north.producoes.controller.dto.response.MetaInstagramBusinessAccountResponseDTO;

public record MetaPageResponseDTO(
        String id,
        String name,
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("instagram_business_account")
        MetaInstagramBusinessAccountResponseDTO instagramBusinessAccount
) {}
