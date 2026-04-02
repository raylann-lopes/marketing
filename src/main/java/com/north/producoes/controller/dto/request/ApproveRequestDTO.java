package com.north.producoes.controller.dto.request;

public record ApproveRequestDTO(
        Long postId,
        String artUrl,
        String artName,
        String caption
) {}
