package com.north.producoes.controller.api;

import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

public interface ApifyContentIdeaApi {

    @Operation(summary = "Gerar ideias de conteúdo para um cliente específico")
    @ApiResponse(responseCode = "200", description = "Ideias de conteúdo geradas com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @PostMapping("/content-ideas/{id}")
    ResponseEntity<ContentIdeaTermsDTO> getContentIdeas(@PathVariable Long id);

    @Operation(summary = "Buscar sinais do Instagram para um cliente específico")
    @ApiResponse(responseCode = "200", description = "Sinais do Instagram encontrados com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @PostMapping("/content-ideas/{id}/signals")
    ResponseEntity<List<ApifyContentIdeaSignalDTO>> searchInstagramSignals(@PathVariable Long id);
}
