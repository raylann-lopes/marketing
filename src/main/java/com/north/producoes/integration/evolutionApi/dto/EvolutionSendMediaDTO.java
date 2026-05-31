package com.north.producoes.integration.evolutionApi.dto;

public record EvolutionSendMediaDTO(
        String number,
        String mediatype,
        String media,
        String caption
) {}
