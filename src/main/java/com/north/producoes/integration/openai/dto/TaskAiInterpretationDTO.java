package com.north.producoes.integration.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskAiInterpretationDTO(
        String title,
        String description,
        String clientName,
        String dateExpires,
        String timeExpires,
        TaskTypeEnum type,
        TaskPriorityEnum priority
) {}
