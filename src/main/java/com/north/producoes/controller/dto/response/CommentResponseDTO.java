package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.CommentEntity;
import com.north.producoes.entity.UserEntity;

import java.time.LocalDateTime;

public record CommentResponseDTO(
        Long id,
        String text,
        String authorName,
        Long authorId,
        LocalDateTime createdAt
) {
    public static CommentResponseDTO from(CommentEntity commentEntity, UserEntity userEntity) {
        return new CommentResponseDTO(
                commentEntity.getId(),
                commentEntity.getText(),
                userEntity.getName(),
                userEntity.getId(),
                commentEntity.getCreatedAt()
        );
    }
}
