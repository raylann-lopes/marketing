package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;

import java.time.LocalDateTime;

public record ApproveResponse(
        Long id,
        PostResponse post,
        String artUrl,
        String artName,
        String caption,
        ApproveStatusEnum status,
        LocalDateTime approvedAt,
        String ApprovedUser
) {
    public static ApproveResponse from(ApproveEntity entity){
        return new ApproveResponse(
                entity.getId(),
                PostResponse.from(entity.getPost()),
                entity.getArtUrl(),
                entity.getArtName(),
                entity.getCaption(),
                entity.getStatus(),
                entity.getApprovedAt(),
                entity.getApprovedUser()
        );
    }
}
