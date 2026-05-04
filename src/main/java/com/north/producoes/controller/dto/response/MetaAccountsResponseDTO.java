package com.north.producoes.controller.dto.response;

import com.north.producoes.integration.meta.dto.MetaPageResponseDTO;

import java.util.List;

public record MetaAccountsResponseDTO(
        List<MetaPageResponseDTO> data
) {}
