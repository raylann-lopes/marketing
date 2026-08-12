package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.TaskStatusEnum;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequestDTO(
        @NotNull
        TaskStatusEnum status
) {
}
