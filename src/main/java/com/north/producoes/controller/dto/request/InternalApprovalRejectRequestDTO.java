package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InternalApprovalRejectRequestDTO(
        @NotBlank(message = "Justificativa é obrigatória")
        @Size(max = 1000, message = "Justificativa deve ter no máximo 1000 caracteres")
        String rejectionReason
) {}
