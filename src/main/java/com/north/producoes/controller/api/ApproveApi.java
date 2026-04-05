package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Aprovações", description = "Gerenciamento de aprovações da agência")
@RequestMapping("/api/post-approvals")
public interface ApproveApi {

    @Operation(summary = "Lista todas as aprovações")
    @ApiResponse(responseCode = "200", description = "Aprovações retornadas com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovações não encontradas")
    @GetMapping("/all")
    ResponseEntity<List<ApproveResponseDTO>> findAll();

    @Operation(summary = "Busca aprovação por ID do post")
    @ApiResponse(responseCode = "200", description = "Aprovação encontrada")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @GetMapping("/{id}")
    ResponseEntity<ApproveResponseDTO> findByPostId(@PathVariable Long id);


    @Operation(summary = "Busca aprovações por status")
    @ApiResponse(responseCode = "200", description = "Aprovações encontradas")
    @ApiResponse(responseCode = "404", description = "Aprovações não encontradas")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @GetMapping("/status/{status}")
    ResponseEntity<List<ApproveResponseDTO>> findApproveByStatus(@PathVariable ApproveStatusEnum status);


    @Operation(summary = "Aprova um post")
    @ApiResponse(responseCode = "200", description = "Post aprovado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PostMapping("/approve/{id}")
    ResponseEntity<ApproveResponseDTO> approvePost(@PathVariable Long id);

    @Operation(summary = "Rejeita um post")
    @ApiResponse(responseCode = "200", description = "Post rejeitado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PostMapping("/reject/{id}")
    ResponseEntity<ApproveResponseDTO> rejectPost(@PathVariable Long id);

    @Operation(summary = "Cria uma nova aprovação")
    @ApiResponse(responseCode = "201", description = "Aprovação criada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PostMapping("/save")
    ResponseEntity<ApproveResponseDTO> saveApprove(@Valid @RequestBody ApproveRequestDTO approve);

    @Operation(summary = "Atualiza um registro de aprovação existente")
    @ApiResponse(responseCode = "200", description = "Registro de aprovação atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Registro de aprovação não encontrado")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PutMapping("/update/{id}")
    ResponseEntity<ApproveResponseDTO> updateApprove(@PathVariable Long id, @Valid @RequestBody ApproveRequestDTO approve);

    @Operation(summary = "Remove uma aprovação pelo ID")
    @ApiResponse(responseCode = "204", description = "Aprovação removida com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @DeleteMapping("/delete/{id}")
    ResponseEntity<Void> deleteApproveById(@PathVariable Long id);
}
