package com.north.producoes.controller.dto.request;

import java.time.LocalDateTime;

public record ApproveByPostRequestDTO(
        LocalDateTime scheduledAt,
        String internalRevisionNotes
) {}
