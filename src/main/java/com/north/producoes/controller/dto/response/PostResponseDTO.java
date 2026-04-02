package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostStatusEnum;

import java.time.LocalDateTime;

public record PostResponseDTO(
        Long id,
        String title,
        String theme,
        String objective,
        PostStatusEnum status,
        LocalDateTime scheduledAt
) {
    public static PostResponseDTO from(PostEntity entity) {
        return new PostResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getTheme(),
                entity.getObjective(),
                entity.getStatus(),
                entity.getScheduledAt()
        );
    }
}
