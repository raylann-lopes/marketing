package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import jakarta.validation.constraints.NotNull;

public record ContentIdeaStatusUpdateRequestDTO(
        @NotNull(message = "Status é obrigatório")
        ContentIdeaStatusEnum status
) {
}
