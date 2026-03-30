package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.PostStatusEnum;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record PostRequest(
        @NotBlank String title,
        @NotBlank String theme,
        @NotBlank String objective,
        @NotBlank PostStatusEnum status,
        @NotBlank LocalDateTime scheduledAt,
        @NotBlank Long clientId,
        @NotBlank Long userId
) {
}
