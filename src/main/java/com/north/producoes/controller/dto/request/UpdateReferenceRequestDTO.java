package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateReferenceRequestDTO(
        @Size(max = 10, message = "Máximo de 10 imagens de referência")
        List<@NotBlank(message = "s3Key não pode ser vazia") String> s3Keys
) {
}
