package com.north.producoes.entity.dto.response;

import com.north.producoes.entity.UserEntity;

public record UserResponse(
        Long id,
        String name,
        String email
) {
    public static UserResponse from(UserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail()
        );
    }
}
