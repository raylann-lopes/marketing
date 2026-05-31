package com.north.producoes.integration.meta.dto;

/**
 * Body enviado ao publicar um container na Meta Graph API.
 * O access_token NÃO é incluído aqui — vai no header Authorization: Bearer.
 */
public record MetaPublishRequestDTO(String creation_id) {}
