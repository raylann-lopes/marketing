package com.north.producoes.controller;

import com.north.producoes.controller.api.MetaAdsApi;
import com.north.producoes.controller.dto.request.MetaAdsAccountInsightRequestDTO;
import com.north.producoes.controller.dto.response.AdsReportSnapshotResponseDTO;
import com.north.producoes.controller.dto.response.MetaAdsAccountOptionResponseDTO;
import com.north.producoes.service.MetaAdsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/admin/meta/ad-account")
public class MetaAdsController implements MetaAdsApi {

    private final MetaAdsService metaAdsService;

    public MetaAdsController(MetaAdsService metaAdsService) {
        this.metaAdsService = metaAdsService;
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<MetaAdsAccountOptionResponseDTO>> getAccount() {
        return ResponseEntity.ok(metaAdsService.findAdsAccounts());
    }

    @Override
    @GetMapping("/clients/{clientId}/report-snapshot")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<AdsReportSnapshotResponseDTO> getAdsReportSnapshot(@PathVariable Long clientId,
                                                                             @RequestParam LocalDate dateStart,
                                                                             @RequestParam LocalDate dateStop) {
        MetaAdsAccountInsightRequestDTO request = new MetaAdsAccountInsightRequestDTO(
                clientId,
                dateStart,
                dateStop
        );
        return ResponseEntity.ok(metaAdsService.metaAdsReportSnapshot(request));
    }
}
