package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ApproveRequestDTO(
        @NotBlank Long postId,
        @NotBlank String artUrl,
        @NotBlank String artName,
        @NotBlank String caption
) {}
