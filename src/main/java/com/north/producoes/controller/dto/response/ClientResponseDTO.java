package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClientResponseDTO(
        Long id,
        String name,
        String email,
        String number,
        String driveLink,
        String voiceTone,
        String niche,
        String whatsappGroupId,
        String whatsappGroupName,
        ClientStatusEnum status,
        BigDecimal monthlyValue,
        LocalDateTime createdAt
) {
    /** Variante para role USER: omite dados financeiros do cliente. */
    public static ClientResponseDTO fromWithoutFinancials(ClientEntity entity) {
        ClientResponseDTO full = from(entity);
        return new ClientResponseDTO(
                full.id(), full.name(), full.email(), full.number(),
                full.driveLink(), full.voiceTone(), full.niche(),
                full.whatsappGroupId(), full.whatsappGroupName(),
                full.status(), null, full.createdAt());
    }

    public static ClientResponseDTO from (ClientEntity entity) {
        return new ClientResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getNumber(),
                entity.getDriveLink(),
                entity.getVoiceTone(),
                entity.getNiche(),
                entity.getWhatsappGroupId(),
                entity.getWhatsappGroupName(),
                entity.getStatus(),
                entity.getMonthlyValue(),
                entity.getCreatedAt()
        );
    }
}
