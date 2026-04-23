package com.north.producoes.controller.dto.response;

public record MediaUrlResponseDTO(
        Long postId,
        String mediaUrl,    // URL presigned GET para o n8n repassar à Meta API (expira em 1h)
        String caption,
        String instagramAccountId,
        String igUserId,
        String accessToken
) {}
