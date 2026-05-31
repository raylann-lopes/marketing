package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.PostRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class PostSchedulerService {

    private final PostRepository postRepository;
    private final InstagramPublishService instagramPublishService;
    private final S3Service s3Service;

    @Scheduled(fixedDelayString = "${scheduler.post.fixed-delay-ms:300000}")
    public void checkAndDispatchScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();

        // JOIN FETCH já carrega approve e client — elimina N+1
        List<PostEntity> scheduledPosts = postRepository.findByStatusWithApproval(PostStatusEnum.SCHEDULE);

        log.info("[Scheduler] Rodando às {} | posts SCHEDULE: {}", now, scheduledPosts.size());

        for (PostEntity post : scheduledPosts) {
            LocalDateTime scheduledAt = post.getScheduledAt();
            if (scheduledAt == null) {
                log.warn("[Scheduler] Post ID {} sem scheduledAt. Ignorando.", post.getId());
                continue;
            }
            if (!scheduledAt.isBefore(now) && !scheduledAt.isEqual(now)) {
                log.info("[Scheduler] Post ID {} aguardando horário: {}", post.getId(), scheduledAt);
                continue;
            }
            dispatchPost(post);
        }
    }

    @Transactional
    public void dispatchPost(PostEntity post) {
        // Transição atômica: só prossegue se ESTA instância ganhar a corrida
        int reserved = postRepository.compareAndSetStatus(
                post.getId(), PostStatusEnum.SCHEDULE, PostStatusEnum.IN_PRODUCTION);

        if (reserved == 0) {
            log.info("[Scheduler] Post ID {} já reservado por outra instância. Ignorando.", post.getId());
            return;
        }

        log.info("[Scheduler] Post ID {} reservado para publicação.", post.getId());

        try {
            ApproveEntity approve = post.getApprove();

            if (approve == null || approve.getStatus() != ApproveStatusEnum.APPROVE) {
                log.warn("[Scheduler] Post ID {} sem aprovação APPROVE. Revertendo para SCHEDULE.",
                        post.getId());
                postRepository.compareAndSetStatus(
                        post.getId(), PostStatusEnum.IN_PRODUCTION, PostStatusEnum.SCHEDULE);
                return;
            }

            String mediaUrl = s3Service.resolveReadUrl(approve.getArtS3Key());

            // Publicação assíncrona — não bloqueia a thread do scheduler
            instagramPublishService.publishAsync(post.getId(), post.getClient().getId(),
                    mediaUrl, approve.getCaption());

        } catch (Exception e) {
            log.error("[Scheduler] Erro ao despachar post ID {} — {}. Revertendo para SCHEDULE.",
                    post.getId(), e.getMessage(), e);
            postRepository.compareAndSetStatus(
                    post.getId(), PostStatusEnum.IN_PRODUCTION, PostStatusEnum.SCHEDULE);
        }
    }
}
