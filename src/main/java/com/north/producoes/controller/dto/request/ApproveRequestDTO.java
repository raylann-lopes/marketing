package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApproveRequestDTO(
        @NotNull(message = "Post é obrigatório")
        @Positive(message = "PostId deve ser maior que zero")
        Long postId,
        @NotBlank(message = "Arquivo é obrigatório")
        @Size(max = 500, message = "artS3Key deve ter no máximo 500 caracteres")
        String artS3Key,
        @NotBlank(message = "Nome do arquivo é obrigatório")
        @Size(max = 255, message = "artName deve ter no máximo 255 caracteres")
        String artName,
        @NotBlank(message = "Legenda é obrigatória")
        @Size(max = 5000, message = "caption deve ter no máximo 5000 caracteres")
        String caption
) {}
