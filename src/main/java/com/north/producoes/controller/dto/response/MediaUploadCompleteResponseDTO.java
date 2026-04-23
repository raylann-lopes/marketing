package com.north.producoes.controller.dto.response;

public record MediaUploadCompleteResponseDTO(
        Long postId,
        String postStatus,
        boolean webhookDispatched
) {
}
