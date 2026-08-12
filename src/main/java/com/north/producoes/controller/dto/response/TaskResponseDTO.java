package com.north.producoes.controller.dto.response;

import com.north.producoes.entity.TaskEntity;
import com.north.producoes.entity.enums.TaskPriorityEnum;
import com.north.producoes.entity.enums.TaskSourceEnum;
import com.north.producoes.entity.enums.TaskStatusEnum;
import com.north.producoes.entity.enums.TaskTypeEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TaskResponseDTO(
        Long id,
        Long clientId,
        String clientName,
        String title,
        String description,
        LocalDate dateExpires,
        LocalTime timeExpires,
        TaskTypeEnum type,
        TaskPriorityEnum priority,
        TaskStatusEnum status,
        TaskSourceEnum source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponseDTO from(TaskEntity task) {
        Long clientId = task.getClient() != null ? task.getClient().getId() : null;

        return new TaskResponseDTO(
                task.getId(),
                clientId,
                task.getClientName(),
                task.getTitle(),
                task.getDescription(),
                task.getDateExpires(),
                task.getTimeExpires(),
                task.getType(),
                task.getPriority(),
                task.getStatus(),
                task.getSource(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
