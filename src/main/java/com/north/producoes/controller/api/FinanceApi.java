package com.north.producoes.controller.api;

import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Financeiro", description = "Gerenciamento financeiro dos clientes")
@RequestMapping("/api/finance")
public interface FinanceApi {

    @Operation(summary = "Lista todos os registros financeiros")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    ResponseEntity<List<FinanceResponseDTO>> findAll();

    @Operation(summary = "Busca registros financeiros por status")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    @ApiResponse(responseCode = "400", description = "Status inválido")
    @ApiResponse(responseCode = "404", description = "Nenhum registro encontrado com o status informado")
    @GetMapping("/status/{status}")
    ResponseEntity<List<FinanceResponseDTO>> findByStatus(
            @Parameter(description = "Status financeiro") @PathVariable FinanceStatusEnum status);

    @Operation(summary = "Busca registros financeiros por cliente")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum registro encontrado para o cliente informado")
    @GetMapping("/client/{id}")
    ResponseEntity<List<FinanceResponseDTO>> findByClient(@PathVariable Long id);

    @Operation(summary = "Cria um novo registro financeiro")
    @ApiResponse(responseCode = "200", description = "Registro criado com sucesso")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PostMapping("/create")
    ResponseEntity<FinanceResponseDTO> saveFinance(@Valid @RequestBody FinanceEntity finance);

    @Operation(summary = "Atualiza um registro financeiro existente")
    @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    @PatchMapping("/update/{id}")
    ResponseEntity<FinanceResponseDTO> updateFinance(@PathVariable Long id, @Valid @RequestBody FinanceEntity finance);

    @Operation(summary = "Remove um registro financeiro pelo ID")
    @ApiResponse(responseCode = "204", description = "Registro removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    @DeleteMapping("/delete/{id}")
    ResponseEntity<Void> deleteFinanceById(
            @Parameter(description = "ID do registro financeiro") @PathVariable Long id);
}
