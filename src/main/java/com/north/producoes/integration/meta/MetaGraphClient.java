package com.north.producoes.integration.meta;

import com.north.producoes.integration.meta.dto.MetaContainerRequestDTO;
import com.north.producoes.integration.meta.dto.MetaPublishRequestDTO;
import com.north.producoes.controller.dto.response.MetaAccountsResponseDTO;
import com.north.producoes.integration.meta.dto.MetaContainerIdResponseDTO;
import com.north.producoes.integration.meta.dto.MetaContainerStatusResponseDTO;
import com.north.producoes.exception.MetaGraphIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Cliente HTTP para a Meta Graph API.
 *
 * Segurança: o access_token é enviado exclusivamente no header
 * "Authorization: Bearer" — NUNCA como query parameter — para evitar
 * que apareça em logs de proxy/load balancer ou no histórico do navegador.
 */
@Component
@RequiredArgsConstructor
public class MetaGraphClient {

    private static final String PAGE_FIELDS =
            "id,name,access_token,instagram_business_account{id,username,name}";

    private final RestClient metaGraphRestClient;

    @Value("${meta.graph.api-version:v19.0}")
    private String apiVersion;

    @Value("${meta.graph.access-token:}")
    private String accessToken;

    public MetaAccountsResponseDTO findPages() {
        if (!StringUtils.hasText(accessToken)) {
            throw new MetaGraphIntegrationException("Token da Meta Graph API nao configurado.");
        }
        try {
            return metaGraphRestClient.get()
                    .uri("/{apiVersion}/me/accounts?fields={fields}", apiVersion, PAGE_FIELDS)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(MetaAccountsResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao consultar contas na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO createContainer(String igUserId, String imageUrl,
                                                       String caption, String clientAccessToken) {
        validateToken(clientAccessToken);
        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media", apiVersion, igUserId)
                    .header("Authorization", "Bearer " + clientAccessToken)
                    .body(new MetaContainerRequestDTO(imageUrl, caption))
                    .retrieve()
                    .body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao criar container na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO createCarouselItemContainer(
            String igUserId, String imageUrl, String token) {
        validateToken(token);
        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media", apiVersion, igUserId)
                    .header("Authorization", "Bearer " + token)
                    .body(new com.north.producoes.integration.meta.dto.MetaCarouselItemRequestDTO(imageUrl, true))
                    .retrieve().body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao criar item do carrossel na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO createCarouselContainer(
            String igUserId, List<String> childrenIds, String caption, String token) {
        validateToken(token);
        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media", apiVersion, igUserId)
                    .header("Authorization", "Bearer " + token)
                    .body(new com.north.producoes.integration.meta.dto.MetaCarouselContainerRequestDTO("CAROUSEL",
                            String.join(",", childrenIds), caption))
                    .retrieve().body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao criar container pai do carrossel na Meta Graph API.", ex);
        }
    }

    public MetaContainerStatusResponseDTO checkContainerStatus(String igContainerId,
                                                                String clientAccessToken) {
        validateToken(clientAccessToken);
        try {
            return metaGraphRestClient.get()
                    .uri("/{apiVersion}/{igContainerId}?fields=status_code", apiVersion, igContainerId)
                    .header("Authorization", "Bearer " + clientAccessToken)
                    .retrieve()
                    .body(MetaContainerStatusResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException(
                    "Falha ao consultar status do container na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO publish(String igUserId, String creationId,
                                               String clientAccessToken) {
        validateToken(clientAccessToken);
        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media_publish", apiVersion, igUserId)
                    .header("Authorization", "Bearer " + clientAccessToken)
                    .body(new MetaPublishRequestDTO(creationId))
                    .retrieve()
                    .body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException(
                    "Falha ao publicar container na Meta Graph API.", ex);
        }
    }

    private void validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new MetaGraphIntegrationException("Token da Meta Graph API nao configurado.");
        }
    }
}
