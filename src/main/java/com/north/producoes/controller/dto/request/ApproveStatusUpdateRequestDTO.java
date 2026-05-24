package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.ApproveStatusEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApproveStatusUpdateRequestDTO(
        @NotNull(message = "status é obrigatório")
        ApproveStatusEnum status,
        @Size(max = 255, message = "approvedUser deve ter no máximo 255 caracteres")
        String approvedUser
) {
}
