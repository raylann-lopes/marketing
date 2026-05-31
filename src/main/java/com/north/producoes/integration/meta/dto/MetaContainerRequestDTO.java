package com.north.producoes.integration.meta.dto;

/**
 * Body enviado ao criar um container de mídia na Meta Graph API.
 * O access_token NÃO é incluído aqui — vai no header Authorization: Bearer.
 */
public record MetaContainerRequestDTO(
        String image_url,
        String caption
) {}
