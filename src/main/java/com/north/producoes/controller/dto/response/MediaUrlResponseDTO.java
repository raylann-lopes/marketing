package com.north.producoes.controller.dto.response;

public record MediaUrlResponseDTO(
        Long approvalId,
        Long postId,
        String mediaUrl,    // URL publica estavel da arte
        String caption,
        String instagramAccountId,
        String igUserId,
        String accessToken
) {}
