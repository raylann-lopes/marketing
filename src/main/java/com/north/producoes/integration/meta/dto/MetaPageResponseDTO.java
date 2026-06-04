package com.north.producoes.integration.meta.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.north.producoes.controller.dto.response.MetaInstagramBusinessAccountResponseDTO;

/**
 * Resposta da Meta Graph API ao listar páginas.
 * accessToken usa WRITE_ONLY: desserializado da resposta da Meta, mas nunca serializado em respostas HTTP.
 */
public record MetaPageResponseDTO(
        String id,
        String name,
        @JsonProperty(value = "access_token", access = JsonProperty.Access.WRITE_ONLY)
        String accessToken,
        @JsonProperty("instagram_business_account")
        MetaInstagramBusinessAccountResponseDTO instagramBusinessAccount
) {}
