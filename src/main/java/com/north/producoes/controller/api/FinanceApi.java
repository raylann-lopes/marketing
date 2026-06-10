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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Financeiro", description = "Gerenciamento financeiro dos clientes (somente ADMIN)")
public interface FinanceApi {

    @Operation(summary = "Retorna previsão financeira do ano")
    @ApiResponse(responseCode = "200", description = "Previsão retornada com sucesso")
    ResponseEntity<ForecastResponseDTO> getForecast(
            @RequestParam(required = false) Integer year);

    @Operation(summary = "Lista todos os registros financeiros")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    ResponseEntity<List<FinanceResponseDTO>> findAll();

    @Operation(summary = "Busca registros financeiros por status")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    ResponseEntity<List<FinanceResponseDTO>> findByStatus(
            @Parameter(description = "Status financeiro") @PathVariable FinanceStatusEnum status);

    @Operation(summary = "Busca registros financeiros por tipo")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    ResponseEntity<List<FinanceResponseDTO>> findByType(
            @Parameter(description = "Tipo financeiro") @PathVariable FinanceTypeEnum type);

    @Operation(summary = "Busca registros financeiros por cliente")
    @ApiResponse(responseCode = "200", description = "Registros encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum registro encontrado para o cliente")
    ResponseEntity<List<FinanceResponseDTO>> findByClient(
            @Parameter(description = "ID do cliente") @PathVariable Long id);

    @Operation(summary = "Cria um novo registro financeiro")
    @ApiResponse(responseCode = "201", description = "Registro criado com sucesso")
    ResponseEntity<FinanceResponseDTO> saveFinance(
            @Valid @RequestBody FinanceRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @Operation(summary = "Atualiza um registro financeiro")
    @ApiResponse(responseCode = "200", description = "Registro atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    ResponseEntity<FinanceResponseDTO> updateFinance(
            @Parameter(description = "ID do registro") @PathVariable Long id,
            @Valid @RequestBody FinanceRequestDTO request);

    @Operation(summary = "Remove um registro financeiro pelo ID")
    @ApiResponse(responseCode = "204", description = "Registro removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro não encontrado")
    ResponseEntity<Void> deleteFinanceById(
            @Parameter(description = "ID do registro") @PathVariable Long id);
}
