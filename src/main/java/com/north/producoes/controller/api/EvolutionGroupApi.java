package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.EvolutionGroupLinkRequestDTO;
import com.north.producoes.controller.dto.response.EvolutionGroupResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Evolution Groups", description = "Consulta e vínculo de grupos do WhatsApp via Evolution API (somente ADMIN)")
@RequestMapping("/api/admin/evolution")
@SecurityRequirement(name = "bearerAuth")
public interface EvolutionGroupApi {

    @Operation(summary = "Lista grupos do WhatsApp disponíveis na Evolution")
    @ApiResponse(responseCode = "200", description = "Grupos retornados com sucesso")
    @ApiResponse(responseCode = "502", description = "Falha ao consultar a Evolution API")
    @GetMapping("/groups")
    ResponseEntity<List<EvolutionGroupResponseDTO>> findGroups();

    @Operation(summary = "Vincula um grupo do WhatsApp a um cliente")
    @ApiResponse(responseCode = "200", description = "Grupo vinculado com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente ou grupo não encontrado")
    @ApiResponse(responseCode = "409", description = "Grupo já vinculado a outro cliente")
    @ApiResponse(responseCode = "502", description = "Falha ao consultar a Evolution API")
    @PostMapping("/groups/link")
    ResponseEntity<EvolutionGroupResponseDTO> linkGroupToClient(
            @Valid @RequestBody EvolutionGroupLinkRequestDTO request);
}
