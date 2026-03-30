package com.north.producoes.controller.api;

import com.north.producoes.entity.dto.request.ClientRequest;
import com.north.producoes.entity.dto.response.ClientResponse;
import com.north.producoes.entity.enums.ClientStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Clientes", description = "Gerenciamento de clientes da agência")
@RequestMapping("/api/clients")
public interface ClientApi {

    @Operation(summary = "Lista todos os clientes")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    ResponseEntity<List<ClientResponse>> findAll();

    @Operation(summary = "Busca cliente por email")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @GetMapping("/email/{email}")
    ResponseEntity<ClientResponse> findByEmail(
            @Parameter(description = "Email do cliente") @PathVariable String email);

    @Operation(summary = "Busca cliente por telefone")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @GetMapping("/number/{number}")
    ResponseEntity<ClientResponse> findByNumber(
            @Parameter(description = "Telefone do cliente") @PathVariable String number);

    @Operation(summary = "Busca clientes por status")
    @ApiResponse(responseCode = "200", description = "Clientes encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum cliente encontrado com o status informado")
    @GetMapping("/status/{status}")
    ResponseEntity<List<ClientResponse>> findByStatus(
            @Parameter(description = "Status do cliente") @PathVariable ClientStatusEnum status);

    @Operation(summary = "Cadastra um novo cliente")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso")
    @ApiResponse(responseCode = "409", description = "Cliente já cadastrado")
    @PostMapping
    ResponseEntity<ClientResponse> saveClient(@Valid @RequestBody ClientRequest request);

    @Operation(summary = "Atualiza um cliente existente")
    @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @PutMapping("/update/{id}")
    ResponseEntity<ClientResponse> updateClient(
            @Parameter(description = "ID do cliente") @PathVariable Long id,
            @Valid @RequestBody ClientRequest request);

    @Operation(summary = "Remove um cliente pelo ID")
    @ApiResponse(responseCode = "204", description = "Cliente removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @DeleteMapping("/id/{id}")
    ResponseEntity<Void> deleteById(
            @Parameter(description = "ID do cliente") @PathVariable Long id);
}
