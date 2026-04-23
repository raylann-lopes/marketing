package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

// Apenas ADMIN acessa — reforçado via @PreAuthorize no controller
@Tag(name = "Configuração de Contas", description = "Gerenciamento de credenciais externas por cliente (somente ADMIN)")
@RequestMapping("/api/admin/account-config")
@SecurityRequirement(name = "bearerAuth")
public interface AccountConfigApi {

    @Operation(summary = "Configura credenciais da conta Instagram/Graph para um cliente")
    @ApiResponse(responseCode = "201", description = "Configuração criada com sucesso")
    @ApiResponse(responseCode = "409", description = "Configuração já existe para este cliente")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @PostMapping
    ResponseEntity<AccountConfigResponseDTO> configure(
            @Valid @RequestBody AccountConfigRequestDTO request,
            Principal principal);

    @Operation(summary = "Retorna a configuração de conta de um cliente")
    @ApiResponse(responseCode = "200", description = "Configuração encontrada")
    @ApiResponse(responseCode = "404", description = "Configuração não encontrada")
    @GetMapping("/client/{clientId}")
    ResponseEntity<AccountConfigResponseDTO> findByClientId(
            @Parameter(description = "ID do cliente") @PathVariable Long clientId);
}
