package com.north.producoes.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ForecastResponseDTO(
    List<ClientForecastDTO> clients,
    List<MonthSummaryDTO> months,
    BigDecimal monthlyTotal,
    BigDecimal annualTotal,
    int year
) {
    public record ClientForecastDTO(
        Long clientId,
        String clientName,
        String niche,
        BigDecimal monthlyValue,
        String status,
        Map<String, ClientMonthDTO> monthData
    ) {}

    public record ClientMonthDTO(BigDecimal expected, BigDecimal received, BigDecimal pending) {}

    public record MonthSummaryDTO(
        String monthKey,
        String monthLabel,
        BigDecimal expected,
        BigDecimal received,
        BigDecimal pending
    ) {}
}
