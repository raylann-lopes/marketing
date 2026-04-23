package com.north.producoes.controller;

import com.north.producoes.controller.api.MediaApi;
import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.service.MediaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@Tag(name = "Media", description = "Upload S3 e URLs de mídia para integração com n8n")
public class MediaController implements MediaApi {

    private final MediaService mediaService;

    /**
     * Gera URL presigned para upload direto do frontend ao S3.
     * O clientId eh sempre derivado pelo servidor a partir do postId.
     * Rota autenticada — qualquer usuário logado pode fazer upload.
     */

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresignedUploadResponseDTO> generateUploadUrl(
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.generateUploadUrl(postId, filename, contentType, user));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaUploadCompleteResponseDTO> markUploadComplete(
            @Valid @RequestBody MediaUploadCompleteRequestDTO request,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.markUploadComplete(request, user));
    }

    /**
     * Gera URL de leitura presigned para o frontend
     * Autenticado por JWT — qualquer usuário logado pode visualizar.bir preview da arte.
     * Expira em 15 minutos (apenas para visualização, não para download permanente).
     */
    @Override
    public ResponseEntity<MediaUrlResponseDTO> getArtPreviewUrl(@RequestParam Long postId,
                                                                @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(mediaService.getArtPreviewUrl(postId, user));
    }

    /**
     * Endpoint interno consumido pelo n8n.
     * Retorna URL presigned de leitura + caption + credenciais do Graph para disparar via Meta API.
     * Protegido por InternalApiKeyFilter (header X-Internal-Api-Key).
     */
    @Override
    public ResponseEntity<MediaUrlResponseDTO> getMediaUrlForN8n(@PathVariable Long postId) {
        return ResponseEntity.ok(mediaService.getMediaUrlForN8n(postId));
    }
}
