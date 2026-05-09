package com.north.producoes.integration.apify.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record ApifyInstagramPostResponseDTO(
        String id,
        String type,
        String shortCode,
        @JsonAlias({"caption", "captionText"})
        String caption,
        List<String> hashtags,
        List<String> mentions,
        String url,
        Integer commentsCount,
        Integer likesCount,
        Integer videoViewCount,
        Integer videoPlayCount,
        String timestamp,
        String ownerUsername,
        String ownerFullName
) {
}
