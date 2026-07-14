package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ContentIdeaStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.response.ContentIdeaResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.service.ApifySearchContentIdeaService;
import com.north.producoes.service.ContentIdeaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/content-ideas")
public class ContentIdeaController {

    private final ContentIdeaService contentIdeaService;
    private final ApifySearchContentIdeaService apifySearchContentIdeaService;
    private final ClientRepository clientRepository;

    @Operation(summary = "Listar todas as ideias de conteúdo")
    @ApiResponse(responseCode = "200", description = "Ideias listadas com sucesso")
    @GetMapping("/all")
    public ResponseEntity<List<ContentIdeaResponseDTO>> findAll() {
        return ResponseEntity.ok(contentIdeaService.findAll());
    }

    @Operation(summary = "Atualizar status de uma ideia (SAVED, DISMISSED, CONVERTED). "
            + "CONVERTED cria a demanda correspondente no board de produção.")
    @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Ideia não encontrada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ContentIdeaResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ContentIdeaStatusUpdateRequestDTO request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(contentIdeaService.updateStatus(id, request.status(), currentUser));
    }

    @Operation(summary = "Executar coleta manual: busca sinais na Apify e gera ideias para o cliente. "
            + "Restrito a ADMIN — cada execução consome créditos da Apify e da OpenAI.")
    @ApiResponse(responseCode = "200", description = "Coleta executada e ideias geradas")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @ApiResponse(responseCode = "409", description = "Coleta já em andamento para o cliente")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    @PostMapping("/collect/{clientId}")
    public ResponseEntity<List<ContentIdeaResponseDTO>> collect(@PathVariable Long clientId) {
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + clientId));
        var ideas = apifySearchContentIdeaService.collectAndGenerateIdeas(client).stream()
                .map(ContentIdeaResponseDTO::from)
                .toList();
        return ResponseEntity.ok(ideas);
    }
}
