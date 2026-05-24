package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.Size;

public record ApproveWhatsAppUpdateRequestDTO(
        @Size(max = 255, message = "stanzaId deve ter no máximo 255 caracteres")
        String stanzaId,
        @Size(max = 100, message = "sentAt deve ter no máximo 100 caracteres")
        String sentAt,
        @Size(max = 1000, message = "whatsappResponseText deve ter no máximo 1000 caracteres")
        String whatsappResponseText,
        @Size(max = 255, message = "approvedUser deve ter no máximo 255 caracteres")
        String approvedUser
){}