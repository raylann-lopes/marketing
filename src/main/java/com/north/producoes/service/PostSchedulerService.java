package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class PostSchedulerService {

    private final PostRepository postRepository;
    private final ApproveRepository approveRepository;
    private final InstagramPublishService instagramPublishService;
    private final S3Service s3Service;

    @Scheduled(fixedDelayString = "${scheduler.post.fixed-delay-ms:300000}")
    public void checkAndDispatchScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<PostEntity> scheduledPosts = postRepository.findByStatus(PostStatusEnum.SCHEDULE);

        log.info("[Scheduler] Rodando às {} | posts com status SCHEDULE: {}",
                now, scheduledPosts.size());

        for (PostEntity post : scheduledPosts) {
            LocalDateTime scheduledAt = post.getScheduledAt();
            if (scheduledAt == null) {
                log.warn("[Scheduler] Post ID {} sem data de agendamento. Ignorando.", post.getId());
                continue;
            }

            if (scheduledAt.isBefore(now) || scheduledAt.isEqual(now)) {
                log.info("[Scheduler] Post ID {} | agendado para {} | publicando agora.",
                        post.getId(), scheduledAt);
                dispatchPost(post);
            } else {
                log.info("[Scheduler] Post ID {} aguardando horário. Agendado: {} | Agora: {}",
                        post.getId(), scheduledAt, now);
            }
        }
    }

    private void dispatchPost(PostEntity post) {
        try {
            List<ApproveEntity> approvals = approveRepository.findByPostId(post.getId());
            if (approvals.isEmpty()) {
                log.error("[Scheduler] Nenhuma aprovação para post ID: {}", post.getId());
                return;
            }

            ApproveEntity approve = approvals.getFirst();

            if (approve.getStatus() != ApproveStatusEnum.APPROVE) {
                log.warn("[Scheduler] Post ID {} sem aprovação APPROVE (status: {}). Ignorando.",
                        post.getId(), approve.getStatus());
                return;
            }

            String mediaUrl = s3Service.resolveReadUrl(approve.getArtS3Key());

            String mediaId = instagramPublishService.publish(
                    post.getClient().getId(),
                    mediaUrl,
                    approve.getCaption()
            );

            post.setStatus(PostStatusEnum.PUBLISHED);
            postRepository.save(post);

            log.info("[Scheduler] ✅ Post ID {} publicado. Instagram Media ID: {}", post.getId(), mediaId);

        } catch (Exception e) {
            log.error("[Scheduler] ❌ Erro ao publicar post ID {} — {}. Retentando no próximo ciclo.",
                    post.getId(), e.getMessage(), e);
        }
    }
}
