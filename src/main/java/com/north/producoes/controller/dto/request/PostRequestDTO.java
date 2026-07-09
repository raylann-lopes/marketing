package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.PostFormatEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record PostRequestDTO(
        @NotBlank(message = "Titulo é obrigatório")
        @Size(max = 255, message = "Titulo deve ter no máximo 255 caracteres")
        String title,
        @NotBlank(message = "Tema é obrigatório")
        @Size(max = 255, message = "Tema deve ter no máximo 255 caracteres")
        String theme,
        @NotBlank(message = "Objetivo é obrigatório")
        @Size(max = 255, message = "Objetivo deve ter no máximo 255 caracteres")
        String objective,
        @NotNull(message = "Status é obrigatório")
        PostStatusEnum status,
        @NotNull(message = "Data de agendamento é obrigatória")
        LocalDateTime scheduledAt,
        Long clientId,
        Long userId,
        Boolean isUrgent,
        
        // Mantido para retrocompatibilidade
        String referenceImageS3Key,
        
        List<String> referenceImageS3Keys,
        
        PostFormatEnum format
) {
}
