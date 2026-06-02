package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.UserRoleEnum;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequestDTO(
    @NotNull(message = "Role é obrigatório")
    UserRoleEnum role
) {}
