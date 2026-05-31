package com.north.producoes.service;

import com.north.producoes.integration.meta.dto.MetaContainerIdResponseDTO;
import com.north.producoes.integration.meta.dto.MetaContainerStatusResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.exception.MetaGraphIntegrationException;
import com.north.producoes.integration.meta.MetaGraphClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publica posts no Instagram diretamente via Meta Graph API.
 *
 * Fluxo de 2 etapas:
 *   1. Cria container de mídia (POST /{ig-user-id}/media)
 *   2. Aguarda container ficar FINISHED (polling até 12 × 3s = 36s)
 *   3. Publica o container (POST /{ig-user-id}/media_publish)
 *
 * Segurança: o accessToken é SEMPRE buscado via AccountConfigService
 * pelo clientId — nunca recebido como parâmetro externo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstagramPublishService {

    private static final int    MAX_POLL_ATTEMPTS = 12;
    private static final long   POLL_INTERVAL_MS  = 3_000L;

    private final MetaGraphClient      metaGraphClient;
    private final AccountConfigService accountConfigService;

    /**
     * Publica uma imagem no Instagram do cliente.
     *
     * @param clientId ID do cliente — determina qual conta Instagram usar
     * @param imageUrl URL pública da imagem (S3)
     * @param caption  Legenda do post
     * @return ID da mídia publicada no Instagram
     */
    public String publish(Long clientId, String imageUrl, String caption) {
        // Isola credenciais por cliente — sem risco de token cruzado
        AccountConfigEntity config = accountConfigService.findByClientId(clientId);

        String igUserId    = config.getIgUserId();
        String accessToken = config.getAccessToken(); // NUNCA logar este valor

        log.info("[Instagram] Iniciando publicação | cliente: {} | ig_user_id: {}", clientId, igUserId);

        // Etapa 1: criar container
        MetaContainerIdResponseDTO container =
                metaGraphClient.createContainer(igUserId, imageUrl, caption, accessToken);

        String creationId = container.id();
        log.info("[Instagram] Container criado: {}", creationId);

        // Etapa 2: aguardar FINISHED com polling
        awaitContainerReady(creationId, accessToken, clientId);

        // Etapa 3: publicar
        MetaContainerIdResponseDTO published =
                metaGraphClient.publish(igUserId, creationId, accessToken);

        log.info("[Instagram] Publicado! Media ID: {} | cliente: {}", published.id(), clientId);
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
                case "FINISHED" -> {
                    return;
                }
                case "ERROR", "EXPIRED" -> throw new MetaGraphIntegrationException(
                        "Container falhou com status '" + code + "' | cliente " + clientId);
                default -> { /* IN_PROGRESS — próxima iteração */ }
            }
        }

        throw new MetaGraphIntegrationException(
                "Timeout: container " + creationId + " não ficou pronto após "
                + MAX_POLL_ATTEMPTS + " tentativas | cliente " + clientId);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MetaGraphIntegrationException("Publicação interrompida durante polling do container.");
        }
    }
}
