package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApproveRequestDTO(
        @NotNull Long postId,
        @NotBlank String artUrl,
        @NotBlank String artName,
        @NotBlank String caption
) {}
