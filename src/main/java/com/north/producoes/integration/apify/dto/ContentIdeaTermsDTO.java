package com.north.producoes.integration.apify.dto;

import java.util.List;

public record ContentIdeaTermsDTO(
        List<String> hashtag,
        List<String> searchTerms
) {
}
