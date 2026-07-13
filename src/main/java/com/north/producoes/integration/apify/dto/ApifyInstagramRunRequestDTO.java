package com.north.producoes.integration.apify.dto;

/**
 * Input do Actor apify/instagram-api-scraper.
 * Estratégia validada: buscar PERFIS do nicho (searchType=user) e raspar os
 * posts deles — contas estabelecidas têm engajamento real. A página de
 * hashtag só retorna posts recém-publicados (quase sem likes) e o campo
 * search com searchType=hashtag responde "no_items" de forma consistente.
 */
public record ApifyInstagramRunRequestDTO(
        String search,
        String searchType,
        String resultsType,
        Integer searchLimit,
        Integer resultsLimit
) {
}
