package com.north.producoes.controller.dto.response;

import java.time.LocalDate;

public record AdsReportSnapshotResponseDTO(
        Long clientId,
        String clientName,
        String accountId,
        String accountName,
        LocalDate dateStart,
        LocalDate dateStop,
        MetaAdsReportSummaryResponseDTO summary,
        MetaAdsReportActionsResponseDTO actions
) {
    public static AdsReportSnapshotResponseDTO from(
            Long clientId,
            String clientName,
            MetaAdsAccountInsightResponseDTO insight
    ) {
        return new AdsReportSnapshotResponseDTO(
                clientId,
                clientName,
                insight.accountId(),
                insight.accountName(),
                insight.dateStart(),
                insight.dateStop(),
                MetaAdsReportSummaryResponseDTO.from(insight),
                MetaAdsReportActionsResponseDTO.from(insight.actions())
        );
    }
}
