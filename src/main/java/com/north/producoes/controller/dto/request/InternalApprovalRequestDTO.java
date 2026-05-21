package com.north.producoes.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record InternalApprovalRequestDTO(
    LocalDateTime scheduledAt,
    String internalRevisionNotes
) {}
