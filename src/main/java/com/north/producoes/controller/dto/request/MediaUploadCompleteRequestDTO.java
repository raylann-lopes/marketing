package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MediaUploadCompleteRequestDTO(
        @NotNull(message = "Post é obrigatório")
        @Positive(message = "postId deve ser maior que zero")
        Long postId,

        @NotBlank(message = "Chave do S3 é obrigatória")
        @Size(max = 500, message = "s3Key deve ter no máximo 500 caracteres")
        String s3Key,

        @NotBlank(message = "Nome da arte é obrigatório")
        @Size(max = 255, message = "artName deve ter no máximo 255 caracteres")
        String artName
) {
}
