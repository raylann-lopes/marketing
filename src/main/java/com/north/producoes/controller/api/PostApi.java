package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.CaptionRequestDTO;
import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.controller.dto.response.PostResponseDTO;
import com.north.producoes.entity.enums.PostStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Posts", description = "Gerenciamento de posts de clientes")
@RequestMapping("/api/posts")
public interface PostApi {

    @Operation(summary = "Lista todos os posts")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    ResponseEntity<List<PostResponseDTO>> findAll();

    @Operation(summary = "Busca posts por status")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado com o status informado")
    @GetMapping("/status/{status}")
    ResponseEntity<List<PostResponseDTO>> findByStatus(
            @Parameter(description = "Status do post") @PathVariable PostStatusEnum status);

    @Operation(summary = "Busca posts por cliente")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado para o cliente informado")
    @GetMapping("/client/{id}")
    ResponseEntity<List<PostResponseDTO>> findByClient(
            @Parameter(description = "ID do cliente") @PathVariable Long id);

    @Operation(summary = "Busca posts por usuário")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado para o usuário informado")
    @GetMapping("/user/{id}")
    ResponseEntity<List<PostResponseDTO>> findByUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id);

    @Operation(summary = "Busca posts por período de agendamento")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado no período informado")
    @ApiResponse(responseCode = "422", description = "Data de início posterior à data de fim")
    @GetMapping("/scheduled/{scheduledAt}")
    ResponseEntity<List<PostResponseDTO>> findByScheduledAt(
            @Parameter(description = "Data de início") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt,
            @Parameter(description = "Data de fim") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAtBefore);

    @Operation(summary = "Cria um novo post")
    @ApiResponse(responseCode = "200", description = "Post criado com sucesso")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PostMapping("/save")
    ResponseEntity<PostResponseDTO> savePost(@Valid @RequestBody PostRequestDTO post);

    @Operation(summary = "Atualiza um post existente")
    @ApiResponse(responseCode = "200", description = "Post atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @PutMapping("/update/{id}")
    ResponseEntity<PostResponseDTO> updatePost(
            @Parameter(description = "ID do post") @PathVariable Long id,
            @Valid @RequestBody PostRequestDTO post);

    @Operation(summary = "Remove um post pelo ID")
    @ApiResponse(responseCode = "204", description = "Post removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @DeleteMapping("/delete/{id}")
    ResponseEntity<Void> deletePostById(
            @Parameter(description = "ID do post") @PathVariable Long id);

    @Operation(summary = "Gera legenda automaticamente usando IA")
    @ApiResponse(responseCode = "200", description = "Legenda gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @PostMapping("/{id}/generate-caption")
    ResponseEntity<ApproveResponseDTO> generateCaption(
            @Parameter(description = "ID do post") @PathVariable Long id,
            @Valid @RequestBody(required = false) CaptionRequestDTO request);
}
