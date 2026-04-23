package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface MediaApi {
    @Operation(summary = "Gera URL presigned para upload de arte ao S3")
    @ApiResponse(responseCode = "200", description = "URL de upload gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este post")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    @PostMapping("/api/media/upload-url")
    ResponseEntity<PresignedUploadResponseDTO> generateUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user);

    @Operation(summary = "Confirma upload da arte, muda status para WAITING_APPROVAL e dispara webhook n8n")
    @ApiResponse(responseCode = "200", description = "Upload confirmado e status atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este post")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @PostMapping("/api/media/upload-complete")
    ResponseEntity<MediaUploadCompleteResponseDTO> markUploadComplete(
            @Valid @RequestBody MediaUploadCompleteRequestDTO request,
            @AuthenticationPrincipal UserEntity user);

    @Operation(summary = "Gera URL de preview da arte de um post (uso no frontend)")
    @ApiResponse(responseCode = "200", description = "URL de preview gerada com sucesso")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este post")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    @GetMapping("/api/media/art-url")
    ResponseEntity<MediaUrlResponseDTO> getArtPreviewUrl(@RequestParam Long postId, @AuthenticationPrincipal UserEntity user);

    @Operation(summary = "Retorna URL de mídia e credenciais do Graph para o n8n (uso interno)")
    @ApiResponse(responseCode = "200", description = "URL de mídia e credenciais retornadas com sucesso")
    @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este post")
    @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    @ApiResponse(responseCode = "404", description = "Post não encontrado")
    @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    @GetMapping("/api/internal/media-url/{postId}")
    ResponseEntity<MediaUrlResponseDTO> getMediaUrlForN8n(@PathVariable Long postId);
}
