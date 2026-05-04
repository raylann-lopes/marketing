package com.north.producoes.integration.evolutionApi.dto;

public record EvolutionGroupApiResponseDTO(
        String id,
        String subject,
        String pictureUrl,
        Integer size,
        String owner,
        String desc,
        Boolean restrict,
        Boolean announce
) {}
