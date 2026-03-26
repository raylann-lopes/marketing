package com.north.producoes.dto.response;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;

import java.time.LocalDateTime;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String number,
        String driveLink,
        String voiceTone,
        String niche,
        ClientStatusEnum status,
        LocalDateTime createdAt
) {
    public static ClientResponse from (ClientEntity entity) {
        return new ClientResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getNumber(),
                entity.getDriveLink(),
                entity.getVoiceTone(),
                entity.getNiche(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
