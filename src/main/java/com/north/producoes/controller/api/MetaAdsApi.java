package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.response.AdsReportSnapshotResponseDTO;
import com.north.producoes.controller.dto.response.MetaAdsAccountOptionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

public interface MetaAdsApi {
    @GetMapping
    @Operation(summary = "Buscar contas do Meta Ads disponiveis")
    @ApiResponse(responseCode = "200", description = "Contas retornadas com sucesso")
    @ApiResponse(responseCode = "502", description = "Falha ao consultar contas de anuncios")
    ResponseEntity<List<MetaAdsAccountOptionResponseDTO>> getAccount();

    @GetMapping("/clients/{clientId}/report-snapshot")
    ResponseEntity<AdsReportSnapshotResponseDTO> getAdsReportSnapshot(@PathVariable Long clientId,
                                                                      @RequestParam LocalDate dateStart,
                                                                      @RequestParam LocalDate dateStop);
}
