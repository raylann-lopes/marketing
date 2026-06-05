package com.north.producoes.service;

import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.PostStatusEnum;
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

    private final PostRepository     postRepository;
    private final PostDispatchService postDispatchService;

    @Scheduled(fixedDelayString = "${scheduler.post.fixed-delay-ms:120000}")
    public void checkAndDispatchScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();

        // JOIN FETCH carrega approve e client — elimina N+1 queries
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
            // Chama bean externo → proxy Spring ativo → @Transactional funciona
            postDispatchService.dispatch(post);
        }
    }
}
