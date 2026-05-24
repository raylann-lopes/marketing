package com.north.producoes.controller.dto.response;

import java.util.List;
import java.util.Map;

public record ForecastResponseDTO(
    List<ClientForecastDTO> clients,
    List<MonthSummaryDTO> months,
    Double monthlyTotal,
    Double annualTotal,
    int year
) {
    public record ClientForecastDTO(
        Long clientId,
        String clientName,
        String niche,
        Double monthlyValue,
        String status,
        Map<String, ClientMonthDTO> monthData
    ) {}

    public record ClientMonthDTO(Double expected, Double received, Double pending) {}

    public record MonthSummaryDTO(
        String monthKey,
        String monthLabel,
        Double expected,
        Double received,
        Double pending
    ) {}
}
