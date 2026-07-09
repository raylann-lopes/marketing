package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.CaptionRequestDTO;
import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.controller.dto.request.UpdateReferenceRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.controller.dto.response.PostResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Posts", description = "Gerenciamento de posts de clientes")
public interface PostApi {

    @GetMapping
    @Operation(summary = "Lista todos os posts (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    ResponseEntity<List<PostResponseDTO>> findAll();

    @GetMapping("/status/{status}")
    @Operation(summary = "Busca posts por status (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    ResponseEntity<List<PostResponseDTO>> findByStatus(
            @Parameter(description = "Status do post") @PathVariable PostStatusEnum status);

    @GetMapping("/client/{id}")
    @Operation(summary = "Busca posts por cliente (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    ResponseEntity<List<PostResponseDTO>> findByClient(
            @Parameter(description = "ID do cliente") @PathVariable Long id);

    @GetMapping("/user/{id}")
    @Operation(summary = "Busca posts por usuário — ADMIN vê qualquer usuário, USER vê apenas os próprios")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "403", description = "Sem permissão para acessar posts de outro usuário")
    ResponseEntity<List<PostResponseDTO>> findByUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserEntity currentUser);

    @GetMapping("/scheduled/{scheduledAt}")
    @Operation(summary = "Busca posts por período de agendamento (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    ResponseEntity<List<PostResponseDTO>> findByScheduledAt(
            @Parameter(description = "Data de início") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
            @Parameter(description = "Data de fim") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAtBefore);

    @PostMapping("/save")
    @Operation(summary = "Cria um novo post (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Post criado com sucesso")
    ResponseEntity<PostResponseDTO> savePost(@Valid @RequestBody PostRequestDTO post);

    @PutMapping("/update/{id}")
    @Operation(summary = "Atualiza um post existente (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Post atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<PostResponseDTO> updatePost(
            @Parameter(description = "ID do post") @PathVariable Long id,
            @Valid @RequestBody PostRequestDTO post);

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "Remove um post pelo ID (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Post removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<Void> deletePostById(
            @Parameter(description = "ID do post") @PathVariable Long id);

    @PatchMapping("/update-reference/{id}")
    @Operation(summary = "Atualiza a imagem de referência do post (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Referência atualizada com sucesso")
    ResponseEntity<PostResponseDTO> updateReferenceImage(
            @Parameter(description = "ID do post") @PathVariable Long id,
            @Valid @RequestBody(required = false) UpdateReferenceRequestDTO body,
            @Parameter(description = "Chave S3 única (retrocompatibilidade)") @RequestParam(required = false) String s3Key);

    @PostMapping("/{id}/generate-caption")
    @Operation(summary = "Gera legenda automaticamente usando IA")
    @ApiResponse(responseCode = "200", description = "Legenda gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    ResponseEntity<ApproveResponseDTO> generateCaption(
            @Parameter(description = "ID do post") @PathVariable Long id,
            @Valid @RequestBody(required = false) CaptionRequestDTO request);
}
