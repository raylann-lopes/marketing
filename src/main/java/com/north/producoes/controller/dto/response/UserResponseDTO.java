package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        UserRoleEnum role,
        boolean active
) {
    public static UserResponseDTO from(UserEntity entity) {
        return new UserResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getRole(),
                entity.isActive()
        );
    }
}
