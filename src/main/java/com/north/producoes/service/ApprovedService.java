package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ApproveByPostRequestDTO;
import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.controller.dto.request.RejectByPostRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApprovedService {

    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;
    private final S3Service s3Service;
    private final WhatsAppNotificationService whatsAppNotificationService;

    @Transactional(readOnly = true)
    public Optional<ApproveEntity> findById(Long id) {
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum post encontrado com id: " + id);
        }
        return approveRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ApproveEntity> findApproveByStatus(ApproveStatusEnum status) {
        List<ApproveEntity> result = approveRepository.findApproveEntitiesByStatus(status);
        if (result.isEmpty()) {
            throw new ResourceNotFoundException("Nenhum post encontrado com status: " + status);
        }
        return result;
    }

    @Transactional
    public ApproveEntity saveApprove(ApproveRequestDTO dto) {
        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException("Post não encontrado com id: " + dto.postId()));

        ApproveEntity approve = approveRepository.findByPostId(dto.postId()).stream().findFirst()
                .orElse(new ApproveEntity());

        approve.setPost(post);
        applyArts(approve, dto);
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

    @Transactional
    public ApproveEntity updateApprove(Long id, ApproveRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum registro de aprovação encontrado com id: " + id));

        PostEntity post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Post não encontrado com id: " + dto.postId()));

        existing.setPost(post);
        applyArts(existing, dto);
        existing.setCaption(dto.caption());

        return approveRepository.save(existing);
    }

    /**
     * Popula carouselArts a partir de dto.arts() (1 a 10 imagens) ou, na ausência,
     * mantém o fluxo single-image legado via artS3Key/artName.
     */
    private void applyArts(ApproveEntity approve, ApproveRequestDTO dto) {
        List<ApproveArts.ArtRef> arts = dto.arts() == null ? null :
                dto.arts().stream()
                        .map(item -> new ApproveArts.ArtRef(item.s3Key(), item.artName()))
                        .toList();
        ApproveArts.replace(s3Service, approve, arts, dto.artS3Key(), dto.artName());
    }

    public String getS3KeyByPostId(Long postId) {
        List<ApproveEntity> approvals = approveRepository.findByPostId(postId);
        if (approvals.isEmpty()) {
            throw new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId);
        }
        return approvals.getFirst().getArtS3Key();
    }

    @Transactional
    public void deleteApproveById(Long id) {
        if (!approveRepository.existsById(id)) {
            throw new ResourceNotFoundException("Nenhum registro de aprovação encontrado com id: " + id);
        }
        approveRepository.deleteById(id);
    }

    @Transactional
    public ApproveEntity updateWhatsappMetadata(Long id, ApproveWhatsAppUpdateRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum registro de aprovação encontrado com id: " + id));

        if (dto.stanzaId() != null)             existing.setWhatsappStanzaId(dto.stanzaId());
        if (dto.sentAt() != null)               existing.setWhatsappSentAt(dto.sentAt());
        if (dto.whatsappResponseText() != null) existing.setWhatsappResponseText(dto.whatsappResponseText());
        if (dto.approvedUser() != null)         existing.setApprovedUser(dto.approvedUser());

        return approveRepository.save(existing);
    }

    /**
     * Atualiza status de aprovação via callback externo (ex.: integração WhatsApp).
     * APPROVE → muda post para SCHEDULE.
     * REJECTED → notifica grupo WhatsApp diretamente.
     */
    @Transactional
    public ApproveEntity updateApprovalStatus(Long id, ApproveStatusUpdateRequestDTO dto) {
        ApproveEntity existing = approveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum registro de aprovação encontrado com id: " + id));

        if (dto.status() == ApproveStatusEnum.PENDING) {
            throw new IllegalArgumentException("Endpoint interno aceita apenas status APPROVE ou REJECTED");
        }

        existing.setStatus(dto.status());

        if (dto.status() == ApproveStatusEnum.APPROVE) {
            existing.setApprovedAt(LocalDateTime.now());
            existing.setApprovedUser(dto.approvedUser() != null ? dto.approvedUser() : "system");

            PostEntity post = existing.getPost();
            if (post != null) {
                rescheduleIfPastDue(post);
                post.setStatus(PostStatusEnum.SCHEDULE);
                postRepository.save(post);
            }

        } else if (dto.status() == ApproveStatusEnum.REJECTED) {
            existing.setApprovedAt(null);
            existing.setApprovedUser("");

            PostEntity post = existing.getPost();
            if (post != null) {
                whatsAppNotificationService.sendRejectionNotification(
                        post.getClient().getId(), post.getId(), existing.getWhatsappResponseText());
            }
        }

        return approveRepository.save(existing);
    }


    @Transactional
    public ApproveResponseDTO approveByPostId(Long postId, String username, ApproveByPostRequestDTO dto) {
        ApproveEntity existing = approveRepository.findByPostId(postId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId));

        existing.setStatus(ApproveStatusEnum.APPROVE);
        existing.setApprovedAt(LocalDateTime.now());
        existing.setApprovedUser(username);
        existing.setRejectionReason(null);
        existing.setRejectedAt(null);
        existing.setRejectedBy(null);

        PostEntity post = existing.getPost();
        if (post != null) {
            if (dto != null && dto.scheduledAt() != null) {
                post.setScheduledAt(dto.scheduledAt());
            }
            rescheduleIfPastDue(post);
            post.setStatus(PostStatusEnum.SCHEDULE);
            postRepository.save(post);
        }

        if (dto != null) {
            existing.setInternalRevisionNotes(dto.internalRevisionNotes());
        }

        return ApproveResponseDTO.from(approveRepository.save(existing));
    }

    @Transactional
    public ApproveResponseDTO rejectByPostId(Long postId, String username, RejectByPostRequestDTO dto) {
        ApproveEntity existing = approveRepository.findByPostId(postId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma aprovação encontrada para o post ID: " + postId));

        existing.setStatus(ApproveStatusEnum.REJECTED);
        existing.setRejectedAt(LocalDateTime.now());
        existing.setRejectedBy(username);
        existing.setRejectionReason(dto != null ? dto.rejectionReason() : null);
        existing.setApprovedAt(null);
        existing.setApprovedUser("");

        PostEntity post = existing.getPost();
        if (post != null) {
            post.setStatus(PostStatusEnum.REJECTED);
            postRepository.save(post);
        }

        return ApproveResponseDTO.from(approveRepository.save(existing));
    }

    // DTO-level methods used by ApproveController
    @Transactional(readOnly = true)
    public List<ApproveResponseDTO> findAll() {
        return approveRepository.findAll().stream().map(ApproveResponseDTO::from).toList();
    }

    /**
     * Busca aprovação por postId com verificação de ownership.
     * ADMIN vê qualquer aprovação; USER só vê aprovações de posts próprios.
     */
    @Transactional(readOnly = true)
    public ApproveResponseDTO findByPostId(Long postId, UserEntity currentUser) {
        ApproveEntity approve = approveRepository.findByPostId(postId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma aprovação encontrada para o post: " + postId));

        if (currentUser.getRole() != UserRoleEnum.ADMIN) {
            boolean owns = postRepository.existsByIdAndUserId(postId, currentUser.getId());
            if (!owns) {
                throw new AccessDeniedException("Sem permissão para acessar esta aprovação.");
            }
        }
        return ApproveResponseDTO.from(approve);
    }

    /** @deprecated Usar findByPostId(postId, currentUser) com verificação de ownership */
    @Transactional(readOnly = true)
    public ApproveResponseDTO findByPostId(Long postId) {
        return approveRepository.findByPostId(postId).stream()
                .findFirst()
                .map(ApproveResponseDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma aprovação encontrada para o post: " + postId));
    }

    @Transactional(readOnly = true)
    public List<ApproveResponseDTO> findByStatus(ApproveStatusEnum status) {
        return approveRepository.findApproveEntitiesByStatus(status).stream()
                .map(ApproveResponseDTO::from).toList();
    }

    @Transactional
    public ApproveResponseDTO saveApproveDTO(ApproveRequestDTO dto) {
        return ApproveResponseDTO.from(saveApprove(dto));
    }

    @Transactional
    public ApproveResponseDTO updateApproveDTO(Long id, ApproveRequestDTO dto) {
        return ApproveResponseDTO.from(updateApprove(id, dto));
    }

    /**
     * Se a data agendada já passou, reagenda para HOJE no mesmo horário.
     * Se o horário de hoje também já passou, agenda para daqui a 1 minuto.
     */
    private void rescheduleIfPastDue(PostEntity post) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = post.getScheduledAt();
        if (scheduledAt == null || !scheduledAt.toLocalDate().isBefore(now.toLocalDate())) return;

        LocalDateTime todayAtSameTime = now
                .withHour(scheduledAt.getHour())
                .withMinute(scheduledAt.getMinute())
                .withSecond(0)
                .withNano(0);

        if (todayAtSameTime.isBefore(now)) {
            todayAtSameTime = now.plusMinutes(1);
        }

        post.setScheduledAt(todayAtSameTime);
    }

}
