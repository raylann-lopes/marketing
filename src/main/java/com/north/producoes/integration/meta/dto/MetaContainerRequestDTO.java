package com.north.producoes.integration.meta.dto;

public record MetaContainerRequestDTO(
        String image_url,
        String caption,
        String access_token
) {}
