package com.north.producoes.integration.apify.dto;

import java.util.List;

public record ApifyContentIdeaSignalDTO(
        String caption,
        Integer likes,
        Integer comments,
        String timestamp,
        String type,
        List<String> hashtags,
        Double score
) {
}
