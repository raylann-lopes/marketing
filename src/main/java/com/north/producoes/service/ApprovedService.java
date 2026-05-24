package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.controller.dto.request.InternalApprovalRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApprovedService {

    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;
    private final S3Service s3Service;
    private final N8nWebhookService n8nWebhookService;

    public Optional<ApproveEntity> findById(Long id) {
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum post encontrado com id: " + id);
        }
        return approveRepository.findById(id);
    }

    public List<ApproveEntity> findApproveByStatus(ApproveStatusEnum status) {
        List<ApproveEntity> approveStatus = approveRepository.findApproveEntitiesByStatus(status);
        if (approveStatus.isEmpty()) {
            throw new ResourceNotFoundException("Nenhum post encontrado com status: " + status);
        }
        return approveStatus;
    }

    public ApproveEntity saveApprove(ApproveRequestDTO dto) {
        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + dto.postId()));

        // Verifica se já existe uma aprovação para este post para evitar erro de UNIQUE constraint
        ApproveEntity approve = approveRepository.findByPostId(dto.postId()).stream().findFirst()
                .orElse(new ApproveEntity());
        
        approve.setPost(post);
        approve.setArtS3Key(normalizePublicS3Key(dto.artS3Key()));
        approve.setArtName(dto.artName());
        approve.setCaption(dto.caption());
        approve.setStatus(ApproveStatusEnum.PENDING);
        
        if (approve.getId() == null) {
            approve.setApprovedUser("");
        }

        return approveRepository.save(approve);
    }

    public ApproveEntity findByStanzaId(String stanzaId) {
        return approveRepository.findByWhatsappStanzaId(stanzaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum registro de aprovação encontrado com WhatsApp Stanza ID: " + stanzaId));
    }

    public ApproveEntity updateApprove(Long id, ApproveRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id));

        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + dto.postId()));

        existing.setPost(post);
        existing.setArtS3Key(normalizePublicS3Key(dto.artS3Key()));
        existing.setArtName(dto.artName());
        existing.setCaption(dto.caption());

        return approveRepository.save(existing);
    }

    public String getS3KeyByPostId(Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        return approvals.getFirst().getArtS3Key();
    }

    public void deleteApproveById(Long id) {
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id);
        }
        approveRepository.deleteById(id);
    }

    public ApproveEntity updateWhatsappMetadata(Long id, ApproveWhatsAppUpdateRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id));

        if (dto.stanzaId() != null) existing.setWhatsappStanzaId(dto.stanzaId());
        if (dto.sentAt() != null) existing.setWhatsappSentAt(dto.sentAt());
        if (dto.whatsappResponseText() != null) existing.setWhatsappResponseText(dto.whatsappResponseText());
        if (dto.approvedUser() != null) existing.setApprovedUser(dto.approvedUser());

        return approveRepository.save(existing);
    }

    public ApproveEntity updateApprovalStatus(Long id, ApproveStatusUpdateRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id));

        if (dto.status() == ApproveStatusEnum.PENDING) {
            throw new IllegalArgumentException("Endpoint interno aceita apenas status APPROVE ou REJECTED");
        }

        existing.setStatus(dto.status());
        if (dto.status() == ApproveStatusEnum.APPROVE) {
            existing.setApprovedAt(LocalDateTime.now());
            existing.setApprovedUser(dto.approvedUser() != null ? dto.approvedUser() : "n8n-callback");

            // Quando o cliente aprova, o post entra em agendamento automático
            PostEntity post = existing.getPost();
            if (post != null) {
                post.setStatus(PostStatusEnum.SCHEDULE);
                postRepository.save(post);
            }
        } else if (dto.status() == ApproveStatusEnum.REJECTED) {
            existing.setApprovedAt(null);
            existing.setApprovedUser("");
            
            // Notificar rejeição no grupo (via n8n)
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", "POST_REJECTED_BY_CLIENT");
            payload.put("postId", existing.getPost().getId());
            payload.put("postTitle", existing.getPost().getTitle());
            payload.put("clientName", existing.getPost().getClient().getName());
            payload.put("rejectionReason", existing.getWhatsappResponseText());
            n8nWebhookService.dispatchPostRejected(payload);
        } else {
            existing.setApprovedAt(null);
            existing.setApprovedUser("");
        }

        return approveRepository.save(existing);
    }

    public ApproveEntity internalApproveByPostId(Long postId, String username, InternalApprovalRequestDTO dto) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        ApproveEntity existing = approvals.getFirst();

        existing.setStatus(ApproveStatusEnum.APPROVE);
        existing.setApprovedAt(LocalDateTime.now());
        existing.setApprovedUser(username);

        if (dto != null) {
            PostEntity post = existing.getPost();
            if (dto.scheduledAt() != null) {
                post.setScheduledAt(dto.scheduledAt());
                post.setStatus(PostStatusEnum.FINISHED); // Mudar para FINISHED para que o admin envie ao cliente
                postRepository.save(post);
            }
            existing.setInternalRevisionNotes(dto.internalRevisionNotes());
        }

        existing.setRejectionReason(null);
        existing.setRejectedAt(null);
        existing.setRejectedBy(null);

        return approveRepository.save(existing);
    }

    public ApproveEntity internalRejectByPostId(Long postId, String username, String reason) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        ApproveEntity existing = approvals.getFirst();

        existing.setStatus(ApproveStatusEnum.REJECTED);
        existing.setRejectedAt(LocalDateTime.now());
        existing.setRejectedBy(username);
        existing.setRejectionReason(reason);

        existing.setApprovedAt(null);
        existing.setApprovedUser("");

        // Atualiza o status do Post para REJECTED
        PostEntity post = existing.getPost();
        if (post != null) {
            post.setStatus(PostStatusEnum.REJECTED);
            postRepository.save(post);
        }

        return approveRepository.save(existing);
    }

    // Métodos que retornam DTO diretamente (usados pelo ApproveController)
    public List<ApproveResponseDTO> findAll() {
        return approveRepository.findAll().stream().map(ApproveResponseDTO::from).toList();
    }

    public ApproveResponseDTO findByPostId(Long postId) {
        return approveRepository.findByPostId(postId).stream()
                .findFirst()
                .map(ApproveResponseDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma aprovação encontrada para o post: " + postId));
    }

    public List<ApproveResponseDTO> findByStatus(ApproveStatusEnum status) {
        return approveRepository.findApproveEntitiesByStatus(status).stream()
                .map(ApproveResponseDTO::from).toList();
    }

    public ApproveResponseDTO saveApproveDTO(ApproveRequestDTO dto) {
        return ApproveResponseDTO.from(saveApprove(dto));
    }

    public ApproveResponseDTO updateApproveDTO(Long id, ApproveRequestDTO dto) {
        return ApproveResponseDTO.from(updateApprove(id, dto));
    }

    private String normalizePublicS3Key(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("artS3Key e obrigatoria");
        }

        String normalized = s3Key.trim();
        if (!s3Service.isPublicKey(normalized)) {
            throw new IllegalArgumentException(
                    "artS3Key deve usar o prefixo publico " + s3Service.getPublicPrefix() + "/");
        }
        return normalized;
    }
}
