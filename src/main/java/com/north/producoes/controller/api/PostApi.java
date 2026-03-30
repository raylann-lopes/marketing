package com.north.producoes.controller.api;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.dto.response.PostResponse;
import com.north.producoes.entity.enums.PostStatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    ResponseEntity<List<PostResponse>> findAll();

    @Operation(summary = "Busca posts por status")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado com o status informado")
    @GetMapping("/status/{status}")
    ResponseEntity<List<PostResponse>> findByStatus(
            @Parameter(description = "Status do post") @PathVariable @RequestParam PostStatusEnum status);

    @Operation(summary = "Busca posts por cliente")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado para o cliente informado")
    @GetMapping("/client/{clientId}")
    ResponseEntity<List<PostResponse>> findByClient(
            @Parameter(description = "Entidade do cliente") @PathVariable @RequestParam ClientEntity clientId);

    @Operation(summary = "Busca posts por usuário")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado para o usuário informado")
    @GetMapping("/user/{user}")
    ResponseEntity<List<PostResponse>> findByUser(
            @Parameter(description = "Entidade do usuário") @PathVariable @RequestParam UserEntity user);

    @Operation(summary = "Busca posts por período de agendamento")
    @ApiResponse(responseCode = "200", description = "Posts encontrados")
    @ApiResponse(responseCode = "404", description = "Nenhum post encontrado no período informado")
    @ApiResponse(responseCode = "422", description = "Data de início posterior à data de fim")
    @GetMapping("/scheduled/{scheduledAt}")
    ResponseEntity<List<PostResponse>> findByScheduledAt(
            @Parameter(description = "Data de início") @PathVariable @RequestParam LocalDateTime scheduledAt,
            @Parameter(description = "Data de fim") LocalDateTime scheduledAtBefore);

    @Operation(summary = "Cria um novo post")
    @ApiResponse(responseCode = "200", description = "Post criado com sucesso")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @PostMapping("/save")
    ResponseEntity<PostResponse> savePost(@RequestBody PostEntity post);

    @Operation(summary = "Atualiza um post existente")
    @ApiResponse(responseCode = "200", description = "Post atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @PutMapping("/update/{id}")
    ResponseEntity<PostResponse> updatePost(@RequestBody PostEntity post);

    @Operation(summary = "Remove um post pelo ID")
    @ApiResponse(responseCode = "204", description = "Post removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @DeleteMapping("/delete/{id}")
    ResponseEntity<Void> deletePostById(
            @Parameter(description = "ID do post") @PathVariable @RequestBody Long id);
}
