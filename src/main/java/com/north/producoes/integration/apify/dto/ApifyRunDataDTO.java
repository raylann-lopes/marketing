package com.north.producoes.integration.apify.dto;

public record ApifyRunDataDTO(
        String id,
        String status,
        String defaultDatasetId
) {
}
