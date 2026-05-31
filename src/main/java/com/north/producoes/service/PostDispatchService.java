package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsável por reservar e despachar um post para publicação no Instagram.
 *
 * Separado do PostSchedulerService propositalmente: métodos @Transactional
 * só funcionam via proxy Spring quando chamados de um bean diferente.
 * Chamar @Transactional dentro da mesma classe bypassa o proxy e não cria transação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostDispatchService {

    private final PostRepository          postRepository;
    private final InstagramPublishService instagramPublishService;
    private final S3Service               s3Service;

    /**
     * Reserva o post atomicamente (SCHEDULE → IN_PRODUCTION) e dispara
     * a publicação de forma assíncrona.
     *
     * A transação garante que o compareAndSetStatus seja executado e
     * commitado antes de o thread assíncrono tentar ler o novo status.
     */
    @Transactional
    public void dispatch(PostEntity post) {
        // Transição atômica: previne publicação duplicada em multi-instância
        int reserved = postRepository.compareAndSetStatus(
                post.getId(), PostStatusEnum.SCHEDULE, PostStatusEnum.IN_PRODUCTION);

        if (reserved == 0) {
            log.info("[Dispatch] Post ID {} já processado por outra instância.", post.getId());
            return;
        }

        log.info("[Dispatch] Post ID {} reservado para publicação.", post.getId());

        try {
            ApproveEntity approve = post.getApprove();

            if (approve == null || approve.getStatus() != ApproveStatusEnum.APPROVE) {
                log.warn("[Dispatch] Post ID {} sem aprovação APPROVE (status: {}). Revertendo.",
                        post.getId(), approve != null ? approve.getStatus() : "null");
                postRepository.compareAndSetStatus(
                        post.getId(), PostStatusEnum.IN_PRODUCTION, PostStatusEnum.SCHEDULE);
                return;
            }

            String mediaUrl = s3Service.resolveReadUrl(approve.getArtS3Key());

            // @Async — retorna imediatamente; a transação acima commita antes de o
            // thread assíncrono tentar atualizar o status para PUBLISHED
            instagramPublishService.publishAsync(
                    post.getId(), post.getClient().getId(), mediaUrl, approve.getCaption());

        } catch (Exception e) {
            log.error("[Dispatch] Erro ao despachar post ID {} — {}. Revertendo para SCHEDULE.",
                    post.getId(), e.getMessage(), e);
            postRepository.compareAndSetStatus(
                    post.getId(), PostStatusEnum.IN_PRODUCTION, PostStatusEnum.SCHEDULE);
        }
    }
}
