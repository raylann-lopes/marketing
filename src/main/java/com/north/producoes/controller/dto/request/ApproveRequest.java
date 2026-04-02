package com.north.producoes.controller.dto.request;

public record ApproveRequest(
        Long postId,
        String artUrl,
        String artName,
        String caption
) {
}
