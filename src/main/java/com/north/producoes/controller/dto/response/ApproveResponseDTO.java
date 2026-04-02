package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;

import java.time.LocalDateTime;

public record ApproveResponseDTO(
        Long id,
        PostResponseDTO post,
        String artUrl,
        String artName,
        String caption,
        ApproveStatusEnum status,
        LocalDateTime approvedAt,
        String ApprovedUser
) {
    public static ApproveResponseDTO from(ApproveEntity entity){
        return new ApproveResponseDTO(
                entity.getId(),
                PostResponseDTO.from(entity.getPost()),
                entity.getArtUrl(),
                entity.getArtName(),
                entity.getCaption(),
                entity.getStatus(),
                entity.getApprovedAt(),
                entity.getApprovedUser()
        );
    }
}
