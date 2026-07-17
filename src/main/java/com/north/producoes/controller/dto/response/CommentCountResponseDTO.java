package com.north.producoes.controller.dto.response;

public record CommentCountResponseDTO(
        Long postId,
        Long count
) {}
