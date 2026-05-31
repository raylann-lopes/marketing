package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class PostSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(PostSchedulerService.class);

    private final PostRepository postRepository;
    private final ApproveRepository approveRepository;
    private final N8nWebhookService n8nWebhookService;
    private final S3Service s3Service;

    @Scheduled(fixedDelayString = "${scheduler.post.fixed-delay-ms:1200000}")
    public void checkAndDispatchScheduledPosts() {
        log.debug("Iniciando verificação de posts agendados em {}", LocalDateTime.now());

        List<PostEntity> scheduledPosts = postRepository.findByStatus(PostStatusEnum.SCHEDULE);

        for (PostEntity post : scheduledPosts) {
            if (post.getScheduledAt() != null && post.getScheduledAt().isBefore(LocalDateTime.now())) {
                dispatchPost(post);
            }
        }
    }

    private void dispatchPost(PostEntity post) {
        try {
            List<ApproveEntity> approvals = approveRepository.findByPostId(post.getId());
            if (approvals.isEmpty()) {
                log.error("Nenhuma aprovação encontrada para o post agendado ID: {}", post.getId());
                return;
            }
            ApproveEntity approve = approvals.getFirst();

            if (approve.getStatus() != ApproveStatusEnum.APPROVE) {
                log.warn("Post agendado ID: {} não possui status APPROVE. Pulando disparo.", post.getId());
                return;
            }

            log.info("Disparando post agendado ID: {} - Título: {}", post.getId(), post.getTitle());

            String mediaUrl = s3Service.resolveReadUrl(approve.getArtS3Key());
            Map<String, Object> payload = buildN8nPayload(post, approve, mediaUrl);

            n8nWebhookService.dispatchPublishPost(payload);

            post.setStatus(PostStatusEnum.PUBLISHED);
            postRepository.save(post);
            log.info("Post ID: {} enviado para o n8n e atualizado para PUBLISHED.", post.getId());

        } catch (Exception e) {
            log.error("Erro ao processar post agendado ID: {}: {}", post.getId(), e.getMessage(), e);
        }
    }

    /**
     * Credenciais (igUserId, accessToken) foram removidas deste payload.
     * O n8n deve buscá-las via GET /api/internal/account-config/{client.id}
     * com o header X-Internal-Api-Key — padrão pull é mais seguro que push de credenciais.
     */
    private Map<String, Object> buildN8nPayload(PostEntity post, ApproveEntity approve, String mediaUrl) {
        Map<String, Object> clientPayload = new LinkedHashMap<>();
        clientPayload.put("id", post.getClient().getId());
        clientPayload.put("name", post.getClient().getName());
        clientPayload.put("number", post.getClient().getNumber());

        Map<String, Object> postPayload = new LinkedHashMap<>();
        postPayload.put("id", post.getId());
        postPayload.put("title", post.getTitle());
        postPayload.put("theme", post.getTheme());
        postPayload.put("scheduledAt", post.getScheduledAt().toString());

        Map<String, Object> approvalPayload = new LinkedHashMap<>();
        approvalPayload.put("id", approve.getId());
        approvalPayload.put("caption", approve.getCaption());
        approvalPayload.put("mediaUrl", mediaUrl);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "SCHEDULED_POST_DISPATCH");
        payload.put("dispatchedAt", LocalDateTime.now().toString());
        payload.put("client", clientPayload);
        payload.put("post", postPayload);
        payload.put("approval", approvalPayload);

        return payload;
    }
}
