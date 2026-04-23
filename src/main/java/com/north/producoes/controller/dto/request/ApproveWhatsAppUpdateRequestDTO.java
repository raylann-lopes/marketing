package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.ApproveStatusEnum;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.TimeZoneStorage;

import java.time.LocalDateTime;

public record ApproveWhatsAppUpdateRequestDTO(
        @Size(max = 255, message = "whatsappStanzaId deve ter no máximo 255 caracteres")
        String whatsappStanzaId,
        @Size(max = 100, message = "whatsappSentAt deve ter no máximo 100 caracteres")
        String whatsappSentAt,
        @Size(max = 5000, message = "whatsappResponseText deve ter no máximo 5000 caracteres")
        String whatsappResponseText,
        @TimeZoneStorage
        LocalDateTime approvedAt
){}