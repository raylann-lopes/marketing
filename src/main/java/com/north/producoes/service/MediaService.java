package com.north.producoes.service;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class MediaService {

    private final S3Service s3Service;
    private final AccountConfigService accountConfigService;
    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;
    private final N8nWebhookService n8nWebhookService;

    public PresignedUploadResponseDTO generateUploadUrl(Long postId, String filename, String contentType, UserEntity user) {
        PostEntity post = getAuthorizedPost(postId, user);
        Long clientId = post.getClient().getId();
        String s3Key = s3Service.buildPublicUploadKey(clientId, postId, filename);
        String uploadUrl = s3Service.generateUploadUrl(s3Key, contentType);
        return new PresignedUploadResponseDTO(uploadUrl, s3Key);
    }

    public MediaUrlResponseDTO getArtPreviewUrl(Long postId, UserEntity user) {
        getAuthorizedPost(postId, user);
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma arte encontrada para o post ID: " + postId);
        }
        ApproveEntity approve = approvals.getFirst();
        String previewUrl = s3Service.resolveReadUrl(approve.getArtS3Key());
        return new MediaUrlResponseDTO(approve.getId(), postId, previewUrl, approve.getCaption(), null, null);
    }

    public MediaUrlResponseDTO getMediaUrlForN8n(Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        ApproveEntity approve = approvals.getFirst();
        if (approve.getStatus() != ApproveStatusEnum.APPROVE) {
            throw new ResourceNotFoundException("Post ainda não aprovado para publicação: " + postId);
        }

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + postId));

        String mediaUrl = s3Service.resolveReadUrl(approve.getArtS3Key());
        AccountConfigEntity config = accountConfigService.findByClientId(post.getClient().getId());
        if (isBlank(config.getIgUserId()) || isBlank(config.getAccessToken())) {
            throw new ResourceNotFoundException(
                    "Configuração incompleta para o cliente ID " + post.getClient().getId()
                            + ": igUserId/accessToken obrigatórios");
        }

        return new MediaUrlResponseDTO(
                approve.getId(),
                postId,
                mediaUrl,
                approve.getCaption(),
                config.getIgUserId(),
                config.getAccessToken()
        );
    }

    @Transactional
    public MediaUploadCompleteResponseDTO markUploadComplete(MediaUploadCompleteRequestDTO request, UserEntity user) {
        PostEntity post = getAuthorizedPost(request.postId(), user);

        ApproveEntity approve = approveRepository.findByPostId(post.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    ApproveEntity entity = new ApproveEntity();
                    entity.setPost(post);
                    entity.setCaption("");
                    entity.setApprovedUser("");
                    return entity;
                });

        String normalizedS3Key = normalizeUploadS3Key(request.s3Key());
        approve.setArtS3Key(normalizedS3Key);
        approve.setArtName(request.artName());
        approve.setStatus(ApproveStatusEnum.PENDING);
        approve.setApprovedAt(null);
        approve.setApprovedUser("");
        approve = approveRepository.save(approve);

        post.setStatus(PostStatusEnum.WAITING_APPROVAL);
        postRepository.save(post);

        AccountConfigEntity config = accountConfigService.findByClientId(post.getClient().getId());
        String mediaUrl = s3Service.resolveReadUrl(approve.getArtS3Key());

        Map<String, Object> clientPayload = new LinkedHashMap<>();
        String clientNumber = post.getClient().getNumber();
        clientPayload.put("id", post.getClient().getId());
        clientPayload.put("name", post.getClient().getName());
        clientPayload.put("number", clientNumber);
        clientPayload.put("whatsappNumber", toWhatsappNumber(clientNumber));
        clientPayload.put("whatsappGroupId", post.getClient().getWhatsappGroupId());
        clientPayload.put("whatsappGroupName", post.getClient().getWhatsappGroupName());
        clientPayload.put("niche", post.getClient().getNiche());
        clientPayload.put("voiceTone", post.getClient().getVoiceTone());
        clientPayload.put("status", post.getClient().getStatus().name());

        Map<String, Object> postPayload = new LinkedHashMap<>();
        postPayload.put("id", post.getId());
        postPayload.put("title", post.getTitle());
        postPayload.put("theme", post.getTheme());
        postPayload.put("objective", post.getObjective());
        postPayload.put("status", post.getStatus().name());
        postPayload.put("scheduledAt", post.getScheduledAt().toString());

        Map<String, Object> approvalPayload = new LinkedHashMap<>();
        approvalPayload.put("id", approve.getId());
        approvalPayload.put("status", approve.getStatus().name());
        approvalPayload.put("artS3Key", approve.getArtS3Key());
        approvalPayload.put("artName", approve.getArtName());
        approvalPayload.put("caption", approve.getCaption());
        approvalPayload.put("mediaUrl", mediaUrl);

        Map<String, Object> actorPayload = new LinkedHashMap<>();
        actorPayload.put("id", user.getId());
        actorPayload.put("name", user.getName());
        actorPayload.put("role", user.getRole().name());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "ART_UPLOAD_COMPLETED");
        payload.put("uploadedAt", LocalDateTime.now().toString());
        payload.put("igUserId", config.getIgUserId());
        payload.put("client", clientPayload);
        payload.put("post", postPayload);
        payload.put("approval", approvalPayload);
        payload.put("actor", actorPayload);

        boolean dispatched = n8nWebhookService.dispatchArtUploadCompleted(payload);
        if (!dispatched) {
            throw new AiIntegrationException("Falha ao autenticar/disparar webhook do n8n");
        }

        return new MediaUploadCompleteResponseDTO(post.getId(), post.getStatus().name(), dispatched);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeUploadS3Key(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("s3Key e obrigatoria");
        }

        String normalized = s3Key.trim();
        if (!s3Service.isPublicKey(normalized)) {
            throw new IllegalArgumentException(
                    "s3Key invalida para upload finalizado. Gere uma nova upload-url e utilize o prefixo "
                            + s3Service.getPublicPrefix() + "/");
        }
        return normalized;
    }

    private PostEntity getAuthorizedPost(Long postId, UserEntity user) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + postId));

        if (user == null) {
            throw new AccessDeniedException("Usuario nao autenticado");
        }

        if (user.getRole() == UserRoleEnum.ADMIN) {
            return post;
        }

        boolean hasAccess = postRepository.existsByIdAndUserId(postId, user.getId());
        if (!hasAccess) {
            throw new AccessDeniedException("Sem permissao para acessar este post");
        }

        return post;
    }

    private String toWhatsappNumber(String number) {
        if (number == null || number.isBlank()) {
            return null;
        }
        String digits = number.replaceAll("\\D", "");
        if (digits.startsWith("55")) {
            return digits;
        }
        return "55" + digits;
    }
}
