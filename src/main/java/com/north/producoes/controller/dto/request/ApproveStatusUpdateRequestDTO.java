package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.ApproveStatusEnum;
import jakarta.validation.constraints.NotNull;

public record ApproveStatusUpdateRequestDTO(
        @NotNull(message = "status é obrigatório")
        ApproveStatusEnum status
) {
}
