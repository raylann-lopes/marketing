package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email invalido")
        @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
        String email,
        @NotBlank(message = "Senha é obrigatória")
        @Size(max = 100, message = "Senha deve ter no máximo 100 caracteres")
        String password) {}
