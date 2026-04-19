package com.north.producoes.service;

import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MediaService {

    private final S3Service s3Service;
    private final AccountConfigService accountConfigService;
    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;

    public PresignedUploadResponseDTO generateUploadUrl(Long postId, String filename, String contentType, UserEntity user) {
        PostEntity post = getAuthorizedPost(postId, user);
        Long clientId = post.getClient().getId();
        String s3Key = S3Service.buildS3Key(clientId, postId, filename);
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
        String previewUrl = s3Service.generateDownloadUrl(approve.getArtS3Key());
        return new MediaUrlResponseDTO(postId, previewUrl, approve.getCaption(), null);
    }

    public MediaUrlResponseDTO getMediaUrlForN8n(Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        ApproveEntity approve = approvals.getFirst();

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado: " + postId));

        String mediaUrl = s3Service.generateDownloadUrl(approve.getArtS3Key());
        String instagramAccountId = accountConfigService.getInstagramAccountId(post.getClient().getId());

        return new MediaUrlResponseDTO(
                postId,
                mediaUrl,
                approve.getCaption(),
                instagramAccountId
        );
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
}
