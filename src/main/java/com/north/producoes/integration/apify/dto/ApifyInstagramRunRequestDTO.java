package com.north.producoes.integration.apify.dto;

import java.util.List;

public record ApifyInstagramRunRequestDTO(
        List<String> search,
        String searchType,
        String resultsType,
        Integer resultsLimit
) {
}
