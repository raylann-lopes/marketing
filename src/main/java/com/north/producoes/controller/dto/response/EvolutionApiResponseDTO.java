package com.north.producoes.controller.dto.response;

import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;

import java.util.List;

public record EvolutionApiResponseDTO(
        List<EvolutionGroupApiResponseDTO> groups
) {
}
