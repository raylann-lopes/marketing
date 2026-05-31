package com.north.producoes.integration.meta.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.north.producoes.controller.dto.response.MetaInstagramBusinessAccountResponseDTO;

/**
 * Resposta da Meta Graph API ao listar páginas.
 * accessToken é marcado @JsonIgnore — nunca será serializado em respostas HTTP,
 * sendo usado exclusivamente dentro do servidor para salvar na AccountConfigEntity.
 */
public record MetaPageResponseDTO(
        String id,
        String name,
        @JsonIgnore
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("instagram_business_account")
        MetaInstagramBusinessAccountResponseDTO instagramBusinessAccount
) {}
