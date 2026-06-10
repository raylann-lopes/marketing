package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.ClientRequestDTO;
import com.north.producoes.controller.dto.request.ClientStatusRequestDTO;
import com.north.producoes.controller.dto.response.ClientResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Clientes", description = "Gerenciamento de clientes da agência (somente ADMIN)")
public interface ClientApi {

    @Operation(summary = "Lista todos os clientes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    ResponseEntity<List<ClientResponseDTO>> findAll();

    @Operation(summary = "Busca cliente por email")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    ResponseEntity<ClientResponseDTO> findByEmail(
            @Parameter(description = "Email do cliente") @PathVariable String email);

    @Operation(summary = "Busca cliente por telefone")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    ResponseEntity<ClientResponseDTO> findByNumber(
            @Parameter(description = "Telefone do cliente") @PathVariable String number);

    @Operation(summary = "Busca clientes por status")
    @ApiResponse(responseCode = "200", description = "Clientes encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum cliente encontrado com o status informado")
    ResponseEntity<List<ClientResponseDTO>> findByStatus(
            @Parameter(description = "Status do cliente") @PathVariable ClientStatusEnum status);

    @Operation(summary = "Cadastra um novo cliente")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso")
    @ApiResponse(responseCode = "409", description = "Cliente já cadastrado")
    ResponseEntity<ClientResponseDTO> saveClient(
            @Valid @RequestBody ClientRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @Operation(summary = "Atualiza um cliente existente")
    @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    ResponseEntity<ClientResponseDTO> updateClient(
            @Parameter(description = "ID do cliente") @PathVariable Long id,
            @Valid @RequestBody ClientRequestDTO request);

    @Operation(summary = "Atualiza o status de um cliente")
    @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    ResponseEntity<ClientResponseDTO> updateStatus(
            @Parameter(description = "ID do cliente") @PathVariable Long id,
            @Valid @RequestBody ClientStatusRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @Operation(summary = "Remove um cliente pelo ID")
    @ApiResponse(responseCode = "204", description = "Cliente removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    ResponseEntity<Void> deleteById(
            @Parameter(description = "ID do cliente") @PathVariable Long id);
}
