package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClientRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String name,
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
        String email,
        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 50, message = "Telefone deve ter no máximo 50 caracteres")
        String number,
        @NotBlank(message = "Drive link é obrigatório")
        @Size(max = 500, message = "Drive link deve ter no máximo 500 caracteres")
        String driveLink,
        @Size(max = 255, message = "Tom de voz deve ter no máximo 255 caracteres")
        String voiceTone,
        @NotBlank(message = "Nicho é obrigatório")
        @Size(max = 255, message = "Nicho deve ter no máximo 255 caracteres")
        String niche,
        @NotNull(message = "Valor mensal é obrigatório")
        @DecimalMin(value = "0.0", message = "Valor mensal não pode ser negativo")
        Double monthlyValue
) {}
