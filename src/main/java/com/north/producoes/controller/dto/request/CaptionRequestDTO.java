package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.Size;

public record CaptionRequestDTO(
        @Size(max = 500, message = "artS3Key deve ter no máximo 500 caracteres")
        String artS3Key
) {}
