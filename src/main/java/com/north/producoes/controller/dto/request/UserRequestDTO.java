package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.UserRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequestDTO(
   @NotBlank String name,
   @NotBlank @Email String email,
   @NotBlank String password
) {}
