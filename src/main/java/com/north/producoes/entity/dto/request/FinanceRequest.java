package com.north.producoes.entity.dto.request;

public record FinanceRequest(
        Long client,
        String description,
        Double value,
        String status,
        String expirationDate
) {}
