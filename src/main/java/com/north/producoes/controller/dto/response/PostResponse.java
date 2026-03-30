package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostStatusEnum;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String theme,
        String objective,
        PostStatusEnum status,
        LocalDateTime scheduledAt
) {
    public static PostResponse from(PostEntity entity) {
        return new PostResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getTheme(),
                entity.getObjective(),
                entity.getStatus(),
                entity.getScheduledAt()
        );
    }
}
