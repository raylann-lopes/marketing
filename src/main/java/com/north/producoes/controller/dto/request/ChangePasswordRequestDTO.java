package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        @Size(max = 100, message = "Senha atual deve ter no máximo 100 caracteres")
        String currentPassword,
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 6, max = 100, message = "Nova senha deve ter entre 6 e 100 caracteres")
        String newPassword
) {}
