package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.ContentIdeaEntity;
import com.north.producoes.entity.enums.ContentIdeaPriorityEnum;
import com.north.producoes.entity.enums.ContentIdeaStatusEnum;
import com.north.producoes.entity.enums.ContentIdeiaFormatEnum;

import java.time.LocalDateTime;

public record ContentIdeaResponseDTO(
        Long id,
        Long clientId,
        String clientName,
        String clientNiche,
        String title,
        String hook,
        String theme,
        String objective,
        ContentIdeiaFormatEnum format,
        String reason,
        String sourceTerms,
        String signalSummary,
        Double engagementScore,
        ContentIdeaStatusEnum status,
        ContentIdeaPriorityEnum priority,
        LocalDateTime createdAt
) {
    public static ContentIdeaResponseDTO from(ContentIdeaEntity entity) {
        return new ContentIdeaResponseDTO(
                entity.getId(),
                entity.getClient() != null ? entity.getClient().getId() : null,
                entity.getClient() != null ? entity.getClient().getName() : null,
                entity.getClient() != null ? entity.getClient().getNiche() : null,
                entity.getTitle(),
                entity.getHook(),
                entity.getTheme(),
                entity.getObjective(),
                entity.getFormat(),
                entity.getReason(),
                entity.getSourceTerms(),
                entity.getSignalSummary(),
                entity.getEngagementScore(),
                entity.getStatus(),
                entity.getPriority(),
                entity.getCreatedAt()
        );
    }
}
