package com.north.producoes.controller.dto.request;

public record FinanceRequestDTO(
        Long client,
        String description,
        Double value,
        String status,
        String expirationDate
) {}
