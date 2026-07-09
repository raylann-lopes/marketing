package com.north.producoes.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ApproveRequestDTO(
        @NotNull(message = "Post é obrigatório")
        @Positive(message = "PostId deve ser maior que zero")
        Long postId,

        // Mantido para retrocompatibilidade — quando arts é informado, vira o primeiro item
        @Size(max = 500, message = "artS3Key deve ter no máximo 500 caracteres")
        String artS3Key,
        @Size(max = 255, message = "artName deve ter no máximo 255 caracteres")
        String artName,

        @NotBlank(message = "Legenda é obrigatória")
        @Size(max = 5000, message = "caption deve ter no máximo 5000 caracteres")
        String caption,

        @Size(max = 10, message = "Máximo de 10 imagens permitidas")
        List<@Valid ArtItem> arts
) {
    public record ArtItem(
            @NotBlank(message = "s3Key é obrigatória")
            @Size(max = 500, message = "s3Key deve ter no máximo 500 caracteres") String s3Key,
            @Size(max = 255, message = "artName deve ter no máximo 255 caracteres") String artName
    ) {}
}
