package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequestDTO(
        @NotNull(message = "Post é obrigatório") Long postId,
        @NotBlank(message = "Comentário não pode ser vazio") @Size(max = 2000) String text
) {}
