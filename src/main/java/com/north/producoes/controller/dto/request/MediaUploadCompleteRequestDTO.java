package com.north.producoes.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MediaUploadCompleteRequestDTO(
        @NotNull(message = "Post é obrigatório")
        @Positive(message = "postId deve ser maior que zero")
        Long postId,

        // Mantido para retrocompatibilidade
        @Size(max = 500, message = "s3Key deve ter no máximo 500 caracteres")
        String s3Key,

        // Mantido para retrocompatibilidade
        @Size(max = 255, message = "artName deve ter no máximo 255 caracteres")
        String artName,

        @Size(max = 10, message = "Máximo de 10 imagens permitidas")
        List<@Valid ArtItem> arts
) {
    public record ArtItem(
            @Size(max = 500, message = "s3Key deve ter no máximo 500 caracteres") String s3Key,
            @Size(max = 255, message = "artName deve ter no máximo 255 caracteres") String artName
    ) {}
}
