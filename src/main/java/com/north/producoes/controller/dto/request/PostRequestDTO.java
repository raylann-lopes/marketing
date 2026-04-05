package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.PostStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record PostRequestDTO(
        @NotBlank String title,
        @NotBlank String theme,
        @NotBlank String objective,
        @NotNull PostStatusEnum status,
        @NotNull LocalDateTime scheduledAt,
        @NotNull Long clientId,
        @NotNull Long userId
) {
}
