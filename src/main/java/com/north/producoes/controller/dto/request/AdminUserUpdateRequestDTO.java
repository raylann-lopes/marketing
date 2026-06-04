package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String name,
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 255)
        String email
) {}
