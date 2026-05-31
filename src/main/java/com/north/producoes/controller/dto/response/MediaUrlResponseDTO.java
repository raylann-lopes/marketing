package com.north.producoes.controller.dto.response;

/**
 * URL pública de mídia para preview no frontend.
 * Não contém credenciais — igUserId e accessToken foram removidos.
 */
public record MediaUrlResponseDTO(
        Long approvalId,
        Long postId,
        String mediaUrl,
        String caption
) {}
