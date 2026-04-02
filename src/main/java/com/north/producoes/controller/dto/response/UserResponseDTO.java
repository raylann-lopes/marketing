package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.UserEntity;

public record UserResponseDTO(
        Long id,
        String name,
        String email
) {
    public static UserResponseDTO from(UserEntity entity) {
        return new UserResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getEmail()
        );
    }
}
