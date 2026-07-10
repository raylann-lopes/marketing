package com.north.producoes.integration.evolutionApi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EvolutionSendMediaDTO(
        String number,
        String mediatype,
        String media,
        String caption
) {}
