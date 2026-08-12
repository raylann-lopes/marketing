package com.north.producoes.controller.dto.request;

import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskRequestDTO(
        Long clientId,
        @Size(max = 120)
        String clientName,
        @NotBlank
        @Size(max = 60)
        String title,
        @Size(max = 500)
        String description,
        @NotNull
        LocalDate dateExpires,
        LocalTime timeExpires,
        TaskTypeEnum type,
        TaskPriorityEnum priority
) {}
