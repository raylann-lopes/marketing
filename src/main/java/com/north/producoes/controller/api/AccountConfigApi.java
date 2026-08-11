package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.request.MetaAdsAccountLinkRequestDTO;
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

@Tag(name = "Configuração de Contas", description = "Gerenciamento de credenciais externas por cliente (somente ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public interface AccountConfigApi {

    @PostMapping
    @Operation(summary = "Configura credenciais da conta Instagram/Graph para um cliente")
    @ApiResponse(responseCode = "201", description = "Configuração criada com sucesso")
    @ApiResponse(responseCode = "409", description = "Configuração já existe para este cliente")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    ResponseEntity<AccountConfigResponseDTO> configure(
            @Valid @RequestBody AccountConfigRequestDTO request,
            Principal principal);

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Retorna a configuração de conta de um cliente")
    @ApiResponse(responseCode = "200", description = "Configuração encontrada")
    @ApiResponse(responseCode = "404", description = "Configuração não encontrada")
    ResponseEntity<AccountConfigResponseDTO> findByClientId(
            @Parameter(description = "ID do cliente") @PathVariable Long clientId);

    @PatchMapping("/client/{clientId}/meta-ad-account")
    @Operation(summary = "Salva o ID da conta de anuncio")
    @ApiResponse(responseCode = "200", description = "ID da conta de anuncios salva")
    @ApiResponse(responseCode = "404", description = "Nao foi possivel salvar o id da conta de anuncios")
    @ApiResponse(responseCode = "409", description = "Conta de anuncios já vinculada")
    ResponseEntity<AccountConfigResponseDTO> linkMetaAdAccount(
            @PathVariable Long clientId,
            @Valid @RequestBody MetaAdsAccountLinkRequestDTO request);

    @DeleteMapping("/client/{clientId}")
    @Operation(summary = "Remove a configuração de conta de um cliente")
    @ApiResponse(responseCode = "204", description = "Configuração removida com sucesso")
    @ApiResponse(responseCode = "404", description = "Configuração não encontrada")
    ResponseEntity<Void> deleteByClientId(
            @Parameter(description = "ID do cliente") @PathVariable Long clientId);
}
