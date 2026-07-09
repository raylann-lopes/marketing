package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.ApproveCarouselArtEntity;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

public record ApproveResponseDTO(
        Long id,
        Long postId,
        String artS3Key,
        String artName,
        List<String> artS3Keys,
        String caption,
        ApproveStatusEnum status,
        LocalDateTime approvedAt,
        String approvedUser,
        String whatsappStanzaId,
        String whatsappSentAt,
        String whatsappResponseText,
        String rejectionReason,
        String internalRevisionNotes,
        LocalDateTime rejectedAt,
        String rejectedBy
) {
    public static ApproveResponseDTO from(ApproveEntity entity) {
        List<String> artS3Keys = entity.getCarouselArts() != null ?
                entity.getCarouselArts().stream()
                        .map(ApproveCarouselArtEntity::getS3Key)
                        .toList() :
                List.of();

        return new ApproveResponseDTO(
                entity.getId(),
                entity.getPost().getId(),
                entity.getArtS3Key(),
                entity.getArtName(),
                artS3Keys,
                entity.getCaption(),
                entity.getStatus(),
                entity.getApprovedAt(),
                entity.getApprovedUser(),
                entity.getWhatsappStanzaId(),
                entity.getWhatsappSentAt(),
                entity.getWhatsappResponseText(),
                entity.getRejectionReason(),
                entity.getInternalRevisionNotes(),
                entity.getRejectedAt(),
                entity.getRejectedBy()
        );
    }
}
