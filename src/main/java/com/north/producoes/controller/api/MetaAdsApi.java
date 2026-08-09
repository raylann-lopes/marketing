package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.response.MetaAdsAccountOptionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface MetaAdsApi {
    @GetMapping
    @Operation(summary = "Buscar contas do Meta Ads disponiveis")
    @ApiResponse(responseCode = "200", description = "Contas retornadas com sucesso")
    @ApiResponse(responseCode = "502", description = "Falha ao consultar contas de anuncios")
    ResponseEntity<List<MetaAdsAccountOptionResponseDTO>> getAccount();
}
