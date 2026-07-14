package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.MetaInstagramAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramAccountResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Principal;
import java.util.List;

@Tag(name = "Meta Graph", description = "Consulta de contas Instagram vinculadas à Meta (somente ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public interface MetaGraphApi {

    @GetMapping("/instagram-accounts")
    @Operation(summary = "Lista contas Instagram profissionais disponíveis na Meta")
    @ApiResponse(responseCode = "200", description = "Contas retornadas com sucesso")
    @ApiResponse(responseCode = "502", description = "Falha ao consultar a Meta Graph API")
    ResponseEntity<List<MetaInstagramAccountResponseDTO>> findInstagramAccounts();

    @PostMapping("/instagram-accounts/link")
    @Operation(summary = "Vincula uma conta Instagram da Meta a um cliente")
    @ApiResponse(responseCode = "201", description = "Configuração criada com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente ou conta Instagram não encontrada")
    @ApiResponse(responseCode = "409", description = "Conta Instagram já configurada")
    @ApiResponse(responseCode = "502", description = "Falha ao consultar a Meta Graph API")
    ResponseEntity<AccountConfigResponseDTO> linkInstagramAccount(
            @Valid @RequestBody MetaInstagramAccountLinkRequestDTO request,
            Principal principal);
}
