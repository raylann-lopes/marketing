package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.FinanceRequestDTO;
import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.controller.dto.response.ForecastResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Financeiro", description = "Gerenciamento financeiro dos clientes (somente ADMIN)")
public interface FinanceApi {

    @GetMapping("/forecast")
    @Operation(summary = "Retorna previsão financeira do ano")
    @ApiResponse(responseCode = "200", description = "Previsão retornada com sucesso")
    ResponseEntity<ForecastResponseDTO> getForecast(
            @RequestParam(required = false) Integer year);

    @GetMapping
    @Operation(summary = "Lista registros financeiros, com filtros opcionais de status "
            + "e intervalo de vencimento (from/to no formato yyyy-MM-dd)")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    ResponseEntity<List<FinanceResponseDTO>> findAll(
            @Parameter(description = "Filtra por status (PENDING ou PAY)")
            @RequestParam(required = false) FinanceStatusEnum status,
            @Parameter(description = "Vencimento a partir de (yyyy-MM-dd)")
            @RequestParam(required = false) java.time.LocalDate from,
            @Parameter(description = "Vencimento até (yyyy-MM-dd)")
            @RequestParam(required = false) java.time.LocalDate to);

    @GetMapping("/status/{status}")
    @Operation(summary = "Busca registros financeiros por status")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    ResponseEntity<List<FinanceResponseDTO>> findByStatus(
            @Parameter(description = "Status financeiro") @PathVariable FinanceStatusEnum status);

    @GetMapping("/type/{type}")
    @Operation(summary = "Busca registros financeiros por tipo")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    ResponseEntity<List<FinanceResponseDTO>> findByType(
            @Parameter(description = "Tipo financeiro") @PathVariable FinanceTypeEnum type);

    @GetMapping("/client/{id}")
    @Operation(summary = "Busca registros financeiros por cliente")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum registro encontrado para o cliente")
    ResponseEntity<List<FinanceResponseDTO>> findByClient(
            @Parameter(description = "ID do cliente") @PathVariable Long id);

    @PostMapping
    @Operation(summary = "Cria um novo registro financeiro")
    @ApiResponse(responseCode = "201", description = "Registro criado com sucesso")
    ResponseEntity<FinanceResponseDTO> saveFinance(
            @Valid @RequestBody FinanceRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um registro financeiro")
    @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    ResponseEntity<FinanceResponseDTO> updateFinance(
            @Parameter(description = "ID do registro") @PathVariable Long id,
            @Valid @RequestBody FinanceRequestDTO request);

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um registro financeiro pelo ID")
    @ApiResponse(responseCode = "204", description = "Registro removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    ResponseEntity<Void> deleteFinanceById(
            @Parameter(description = "ID do registro") @PathVariable Long id);
}
