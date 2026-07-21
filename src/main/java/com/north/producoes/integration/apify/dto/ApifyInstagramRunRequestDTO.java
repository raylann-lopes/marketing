package com.north.producoes.integration.apify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Input do Actor apify/instagram-api-scraper. Dois modos, mutuamente
 * exclusivos:
 *
 * <p><b>directUrls</b> (estratégia primária): URLs de página de hashtag do
 * Instagram (https://www.instagram.com/explore/tags/{hashtag}/) — retorna
 * os posts da hashtag diretamente, sem depender de busca por texto.
 *
 * <p><b>search + searchType</b> (fallback): busca por PERFIS do nicho
 * (searchType=user) e raspa os posts deles. Existe porque search com
 * searchType=hashtag respondia "no_items" — na verdade porque os termos
 * gerados pela IA (searchTerms) são frases com espaço, não hashtags
 * válidas; hashtags de verdade (campo hashtags da IA) nunca tinham sido
 * usadas com searchType=hashtag, só como texto de busca de usuário.
 *
 * <p>{@code @JsonInclude(NON_NULL)}: o schema de input do Actor rejeita
 * {@code "search": null} explícito ("Field input.search must be string") —
 * campos não usados precisam ficar ausentes do JSON, não nulos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApifyInstagramRunRequestDTO(
        List<String> directUrls,
        String search,
        String searchType,
        String resultsType,
        Integer searchLimit,
        Integer resultsLimit
) {

    public static ApifyInstagramRunRequestDTO forHashtagUrls(List<String> directUrls, Integer resultsLimit) {
        return new ApifyInstagramRunRequestDTO(directUrls, null, null, "posts", null, resultsLimit);
    }

    public static ApifyInstagramRunRequestDTO forProfileSearch(String search, Integer searchLimit, Integer resultsLimit) {
        return new ApifyInstagramRunRequestDTO(null, search, "user", "posts", searchLimit, resultsLimit);
    }
}
