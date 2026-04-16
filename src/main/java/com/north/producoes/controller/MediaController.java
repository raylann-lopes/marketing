package com.north.producoes.controller;

import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.service.AccountConfigService;
import com.north.producoes.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@Tag(name = "Media", description = "Upload S3 e URLs de mídia para integração com n8n")
public class MediaController {

    private final S3Service s3Service;
    private final AccountConfigService accountConfigService;
    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;

    /**
     * Gera URL presigned para upload direto do frontend ao S3.
     * O frontend faz PUT para uploadUrl e depois salva o s3Key no ApproveRequestDTO.
     * Rota autenticada — qualquer usuário logado pode fazer upload.
     */
    @Operation(summary = "Gera URL presigned para upload de arte ao S3")
    @PostMapping("/api/media/upload-url")
    public ResponseEntity<PresignedUploadResponseDTO> generateUploadUrl(
            @RequestParam Long clientId,
            @RequestParam Long postId,
            @RequestParam String filename,
            @RequestParam String contentType) {

        String s3Key = S3Service.buildS3Key(clientId, postId, filename);
        String uploadUrl = s3Service.generateUploadUrl(s3Key, contentType);
        return ResponseEntity.ok(new PresignedUploadResponseDTO(uploadUrl, s3Key));
    }

    /**
     * Gera URL de leitura presigned para o frontend exi     * Autenticado por JWT — qualquer usuário logado pode visualizar.bir preview da arte.
     * Expira em 15 minutos (apenas para visualização, não para download permanente).
     */
    @Operation(summary = "Gera URL de preview da arte de um post (uso no frontend)")
    @GetMapping("/api/media/art-url")
    public ResponseEntity<MediaUrlResponseDTO> getArtPreviewUrl(@RequestParam Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma arte encontrada para o post ID: " + postId);
        }
        ApproveEntity approve = approvals.getFirst();
        String previewUrl = s3Service.generateDownloadUrl(approve.getArtS3Key());

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + postId));

        return ResponseEntity.ok(new MediaUrlResponseDTO(postId, previewUrl, approve.getCaption(), null));
    }

    /**
     * Endpoint interno consumido pelo n8n.
     * Retorna URL presigned de leitura + caption + instagramAccountId para disparar via Meta API.
     * Protegido por InternalApiKeyFilter (header X-Internal-Api-Key).
     */
    @Operation(summary = "Retorna URL de mídia e dados de publicação para o n8n (uso interno)")
    @GetMapping("/api/internal/media-url/{postId}")
    public ResponseEntity<MediaUrlResponseDTO> getMediaUrlForN8n(@PathVariable Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        ApproveEntity approve = approvals.getFirst();

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + postId));

        String mediaUrl = s3Service.generateDownloadUrl(approve.getArtS3Key());
        String instagramAccountId = accountConfigService.getInstagramAccountId(post.getClient().getId());

        return ResponseEntity.ok(new MediaUrlResponseDTO(
                postId,
                mediaUrl,
                approve.getCaption(),
                instagramAccountId
        ));
    }
}
