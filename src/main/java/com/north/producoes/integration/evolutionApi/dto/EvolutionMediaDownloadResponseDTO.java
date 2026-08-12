package com.north.producoes.integration.evolutionApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionMediaDownloadResponseDTO(
        String mediaType,
        String fileName,
        String mimetype,
        String base64
) {}
