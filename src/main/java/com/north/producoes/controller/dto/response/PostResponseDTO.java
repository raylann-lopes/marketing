package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostFormatEnum;
import com.north.producoes.entity.enums.PostStatusEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PostResponseDTO(
        Long id,
        String title,
        String clientId,
        String theme,
        String objective,
        PostStatusEnum status,
        Boolean isUrgent,
        String referenceImageS3Key,
        List<String> referenceImageS3Keys,
        PostFormatEnum format,
        LocalDateTime scheduledAt
) {
    public static PostResponseDTO from(PostEntity entity) {
        List<String> keys = entity.getCarouselImages() != null ?
                entity.getCarouselImages().stream()
                        .map(com.north.producoes.entity.PostCarouselImageEntity::getS3Key)
                        .collect(Collectors.toList()) :
                List.of();

        return new PostResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getClient().getId().toString(),
                entity.getTheme(),
                entity.getObjective(),
                entity.getStatus(),
                entity.getIsUrgent(),
                entity.getReferenceImageS3Key(),
                keys,
                entity.getFormat(),
                entity.getScheduledAt()
        );
    }
}
