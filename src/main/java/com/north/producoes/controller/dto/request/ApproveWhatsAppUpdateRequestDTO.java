package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.Size;

public record ApproveWhatsAppUpdateRequestDTO(
        @Size(max = 255, message = "stanzaId deve ter no máximo 255 caracteres")
        String stanzaId,
        @Size(max = 100, message = "sentAt deve ter no máximo 100 caracteres")
        String sentAt
){}