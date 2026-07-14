package com.north.producoes.service;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@AllArgsConstructor
public class MediaService {

    private final S3Service s3Service;
    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public PresignedUploadResponseDTO generateUploadUrl(Long postId, String filename,
                                                         String contentType, UserEntity user) {
        PostEntity post = getAuthorizedPost(postId, user);
        String s3Key = s3Service.buildPublicUploadKey(post.getClient().getId(), postId, filename);
        String uploadUrl = s3Service.generateUploadUrl(s3Key, contentType);
        return new PresignedUploadResponseDTO(uploadUrl, s3Key);
    }

    public PresignedUploadResponseDTO generateReferenceUploadUrl(Long postId, String filename,
                                                                   String contentType, UserEntity user) {
        PostEntity post = getAuthorizedPost(postId, user);
        String s3Key = s3Service.buildReferenceKey(post.getClient().getId(), postId, filename);
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
        List<String> previewUrls = approve.getCarouselArts().stream().map(a -> s3Service.resolveReadUrl(a.getS3Key())).toList();
        if (previewUrls.isEmpty()) previewUrls = List.of(previewUrl);
        return new MediaUrlResponseDTO(approve.getId(), postId, previewUrl, previewUrls, approve.getCaption());
    }

    public MediaUrlResponseDTO getReferencePreviewUrl(Long postId, UserEntity user) {
        PostEntity post = getAuthorizedPost(postId, user);
        if (!StringUtils.hasText(post.getReferenceImageS3Key())) {
            throw new ResourceNotFoundException("Nenhuma imagem de referência para o post ID: " + postId);
        }
        String previewUrl = s3Service.resolveReadUrl(post.getReferenceImageS3Key());
        List<String> previewUrls = post.getCarouselImages().stream().map(r -> s3Service.resolveReadUrl(r.getS3Key())).toList();
        if (previewUrls.isEmpty()) previewUrls = List.of(previewUrl);
        return new MediaUrlResponseDTO(null, postId, previewUrl, previewUrls, "Imagem de Referência");
    }

    /**
     * Chamado quando o upload de arte ao S3 é concluído.
     * Salva a aprovação, muda o post para WAITING_APPROVAL e dispara
     * a notificação de aprovação diretamente no WhatsApp do cliente.
     */
    @Transactional
    public MediaUploadCompleteResponseDTO markUploadComplete(MediaUploadCompleteRequestDTO request,
                                                             UserEntity user) {
        PostEntity post = getAuthorizedPost(request.postId(), user);

        ApproveEntity approve = approveRepository.findByPostId(post.getId())
                .stream().findFirst()
                .orElseGet(() -> {
                    ApproveEntity entity = new ApproveEntity();
                    entity.setPost(post);
                    entity.setCaption("");
                    entity.setApprovedUser("");
                    return entity;
                });

        List<ApproveArts.ArtRef> arts = request.arts() == null ? null :
                request.arts().stream()
                        .map(item -> new ApproveArts.ArtRef(item.s3Key(), item.artName()))
                        .toList();
        ApproveArts.replace(s3Service, approve, arts, request.s3Key(), request.artName());

        approve.setStatus(ApproveStatusEnum.PENDING);
        approve.setApprovedAt(null);
        approve.setApprovedUser("");
        // Invalida stanza ID antigo — enquetes anteriores no grupo não serão mais aceitas
        approve.setWhatsappStanzaId(null);
        approve.setWhatsappSentAt(null);
        approve.setWhatsappResponseText(null);
        approve = approveRepository.save(approve);

        post.setStatus(PostStatusEnum.WAITING_APPROVAL);
        postRepository.save(post);

        // Dispara notificação WhatsApp após o commit — evita race condition onde a
        // thread @Async lê a entidade antes do stanzaId ter sido persistido.
        List<String> mediaUrls = !approve.getCarouselArts().isEmpty() ?
                approve.getCarouselArts().stream().map(a -> s3Service.resolveReadUrl(a.getS3Key())).toList() :
                List.of(s3Service.resolveReadUrl(approve.getArtS3Key()));

        final long clientId  = post.getClient().getId();
        final long postId_   = post.getId();
        final long approveId = approve.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                whatsAppNotificationService.sendApprovalRequest(clientId, postId_, approveId, mediaUrls);
            }
        });

        return new MediaUploadCompleteResponseDTO(post.getId(), post.getStatus().name(), true);
    }

    private PostEntity getAuthorizedPost(Long postId, UserEntity user) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + postId));

        if (user == null) throw new AccessDeniedException("Usuário não autenticado");

        // Board compartilhado: o designer (role USER) produz arte para posts
        // criados por qualquer membro da equipe
        return post;
    }
}
