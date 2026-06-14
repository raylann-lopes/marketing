package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.ApproveByPostRequestDTO;
import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.RejectByPostRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Aprovações", description = "Gerenciamento de aprovações de posts")
public interface ApproveApi {

    @GetMapping("/all")
    @Operation(summary = "Lista todas as aprovações (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Aprovações retornadas com sucesso")
    ResponseEntity<List<ApproveResponseDTO>> findAll();

    @GetMapping("/{id}")
    @Operation(summary = "Busca aprovação por ID do post")
    @ApiResponse(responseCode = "200", description = "Aprovação encontrada")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    ResponseEntity<ApproveResponseDTO> findByPostId(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @GetMapping("/status/{status}")
    @Operation(summary = "Busca aprovações por status (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Aprovações encontradas")
    ResponseEntity<List<ApproveResponseDTO>> findApproveByStatus(
            @PathVariable ApproveStatusEnum status);

    @PostMapping("/save")
    @Operation(summary = "Cria uma nova aprovação (somente ADMIN)")
    @ApiResponse(responseCode = "201", description = "Aprovação criada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<ApproveResponseDTO> saveApprove(@Valid @RequestBody ApproveRequestDTO approve);

    @PutMapping("/update/{id}")
    @Operation(summary = "Atualiza um registro de aprovação (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Aprovação atualizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    ResponseEntity<ApproveResponseDTO> updateApprove(
            @PathVariable Long id,
            @Valid @RequestBody ApproveRequestDTO approve);

    @PatchMapping("/approve/{postId}")
    @Operation(summary = "Aprova um post pelo ID do post (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Post aprovado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<ApproveResponseDTO> approveByPostId(
            @PathVariable Long postId,
            @RequestBody(required = false) ApproveByPostRequestDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @PatchMapping("/reject/{postId}")
    @Operation(summary = "Rejeita um post pelo ID do post (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Post rejeitado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<ApproveResponseDTO> rejectByPostId(
            @PathVariable Long postId,
            @RequestBody(required = false) RejectByPostRequestDTO dto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Remove uma aprovação pelo ID (somente ADMIN)")
    @ApiResponse(responseCode = "204", description = "Aprovação removida com sucesso")
    ResponseEntity<Void> deleteApproveById(@PathVariable Long id);
}
