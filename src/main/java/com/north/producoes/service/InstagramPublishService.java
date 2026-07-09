package com.north.producoes.service;

import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.MetaGraphIntegrationException;
import com.north.producoes.integration.meta.MetaGraphClient;
import com.north.producoes.integration.meta.dto.MetaContainerIdResponseDTO;
import com.north.producoes.integration.meta.dto.MetaContainerStatusResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

/**
 * Publica posts no Instagram via Meta Graph API.
 *
 * publishAsync() roda em thread separada (@Async) — não bloqueia o scheduler.
 * O polling de status (Thread.sleep) ocorre fora da thread HTTP/scheduler.
 *
 * Isolamento: accessToken sempre buscado via clientId — nunca parâmetro externo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramPublishService {

    private static final int    MAX_POLL_ATTEMPTS = 12;
    private static final long   POLL_INTERVAL_MS  = 3_000L;

    private final MetaGraphClient      metaGraphClient;
    private final AccountConfigService accountConfigService;
    private final PostRepository       postRepository;

    /**
     * Executa a publicação de forma assíncrona — thread do scheduler é liberada imediatamente.
     * Atualiza o status do post para PUBLISHED ou reverte para SCHEDULE em caso de falha.
     *
     * Sem @Transactional de propósito: o polling da Meta pode levar minutos e
     * seguraria uma conexão do pool o tempo todo. O compareAndSetStatus abre
     * sua própria transação curta.
     */
    @Async
    public void publishAsync(Long postId, Long clientId, List<String> imageUrls, String caption) {
        try {
            String mediaId = publish(clientId, imageUrls, caption);
            postRepository.compareAndSetStatus(
                    postId, PostStatusEnum.IN_PRODUCTION, PostStatusEnum.PUBLISHED);
            log.info("[Instagram] ✅ Post ID {} publicado. Media ID: {}", postId, mediaId);
        } catch (Exception e) {
            log.error("[Instagram] ❌ Falha ao publicar post ID {} — {}. Revertendo para SCHEDULE.",
                    postId, e.getMessage(), e);
            postRepository.compareAndSetStatus(
                    postId, PostStatusEnum.IN_PRODUCTION, PostStatusEnum.SCHEDULE);
        }
    }

    /**
     * Publicação síncrona — use publishAsync para chamadas do scheduler.
     */
    public String publish(Long clientId, List<String> imageUrls, String caption) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new MetaGraphIntegrationException("Nenhuma mídia para publicar | cliente " + clientId);
        }

        AccountConfigEntity config = accountConfigService.findByClientId(clientId);
        String igUserId    = config.getIgUserId();
        String accessToken = config.getAccessToken(); // NUNCA logar

        log.info("[Instagram] Iniciando publicação | cliente: {} | mídias: {}", clientId, imageUrls.size());

        // A quantidade de mídias define o formato: 1 = post simples, 2+ = carrossel.
        // Não usar o campo format do post — um CAROUSEL com 1 imagem falharia para sempre.
        if (imageUrls.size() > 1) {
            return publishCarousel(igUserId, imageUrls, caption, accessToken, clientId);
        }
        return publishSingle(igUserId, imageUrls.get(0), caption, accessToken, clientId);
    }

    private String publishSingle(String igUserId, String imageUrl, String caption, String accessToken, Long clientId) {
        MetaContainerIdResponseDTO container =
                metaGraphClient.createContainer(igUserId, imageUrl, caption, accessToken);

        String creationId = container.id();
        log.info("[Instagram] Container criado: {}", creationId);

        awaitContainerReady(creationId, accessToken, clientId);

        MetaContainerIdResponseDTO published =
                metaGraphClient.publish(igUserId, creationId, accessToken);

        return published.id();
    }

    private String publishCarousel(String igUserId, List<String> imageUrls, String caption, String accessToken, Long clientId) {
        if (imageUrls.size() < 2 || imageUrls.size() > 10) {
            throw new MetaGraphIntegrationException("Carrossel deve ter entre 2 e 10 imagens.");
        }

        // Cria e aguarda cada item individualmente — se um falhar, aborta cedo
        // sem deixar uma fila de containers abandonados na Meta
        List<String> childrenIds = new ArrayList<>();
        for (String url : imageUrls) {
            MetaContainerIdResponseDTO itemContainer = metaGraphClient.createCarouselItemContainer(igUserId, url, accessToken);
            awaitContainerReady(itemContainer.id(), accessToken, clientId);
            childrenIds.add(itemContainer.id());
        }

        MetaContainerIdResponseDTO parentContainer = metaGraphClient.createCarouselContainer(igUserId, childrenIds, caption, accessToken);
        awaitContainerReady(parentContainer.id(), accessToken, clientId);

        MetaContainerIdResponseDTO published = metaGraphClient.publish(igUserId, parentContainer.id(), accessToken);
        return published.id();
    }

    private void awaitContainerReady(String creationId, String accessToken, Long clientId) {
        for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
            sleep(POLL_INTERVAL_MS);

            MetaContainerStatusResponseDTO status =
                    metaGraphClient.checkContainerStatus(creationId, accessToken);

            String code = status.status_code();
            log.debug("[Instagram] Container {} — {} ({}/{})", creationId, code, attempt, MAX_POLL_ATTEMPTS);

            switch (code) {
                case "FINISHED" -> { return; }
                case "ERROR", "EXPIRED" -> throw new MetaGraphIntegrationException(
                        "Container falhou com status '" + code + "' | cliente " + clientId);
                default -> { /* IN_PROGRESS */ }
            }
        }
        throw new MetaGraphIntegrationException(
                "Timeout: container " + creationId + " não ficou pronto | cliente " + clientId);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MetaGraphIntegrationException("Publicação interrompida durante polling.");
        }
    }
}
