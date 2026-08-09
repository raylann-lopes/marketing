package com.north.producoes.integration.meta;

import com.north.producoes.integration.meta.dto.*;
import com.north.producoes.controller.dto.response.MetaAccountsResponseDTO;
import com.north.producoes.exception.MetaGraphIntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
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
    private static final String AD_ACCOUNT_FIELDS =
            "id,account_id,name";
    private static final String AD_ACCOUNT_INSIGHTS_FIELDS =
            "account_id,account_name,spend,reach,impressions,clicks,ctr,cpc,cpm,actions,date_start,date_stop";

    private final RestClient metaGraphRestClient;

    @Value("${meta.graph.api-version:v19.0}")
    private String apiVersion;

    @Value("${meta.graph.access-token:}")
    private String accessToken;

    public MetaAccountsResponseDTO findPages() {
        validateToken(accessToken);

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
            String igUserId, String imageUrl, String clientAcessToken) {
        validateToken(clientAcessToken);
        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media", apiVersion, igUserId)
                    .header("Authorization", "Bearer " + clientAcessToken)
                    .body(new com.north.producoes.integration.meta.dto.MetaCarouselItemRequestDTO(imageUrl, true))
                    .retrieve().body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao criar item do carrossel na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO createCarouselContainer(
            String igUserId, List<String> childrenIds, String caption, String clientAcessToken) {
        validateToken(clientAcessToken);
        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media", apiVersion, igUserId)
                    .header("Authorization", "Bearer " + clientAcessToken)
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

    public MetaAdsAccountResponseDTO getAdAccounts() {
        validateToken(accessToken);

        try {
            return metaGraphRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{apiVersion}/me/adaccounts")
                            .queryParam("fields", AD_ACCOUNT_FIELDS)
                            .build(apiVersion))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(MetaAdsAccountResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao obter contas do Meta Ads.", ex);
        }
    }

    public MetaAdsInsightsResponseDTO getAdAccountInsights(String adAccountId, LocalDate dateStart, LocalDate dateStop) {

        validateToken(accessToken);
        String normalizedAdAccountId = normalizeAdAccountId(adAccountId);

        String timeRangeJson = String.format("{\"since\":\"%s\",\"until\":\"%s\"}", dateStart, dateStop);

        try {
            return metaGraphRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{apiVersion}/act_{adAccountId}/insights")
                            .queryParam("fields", AD_ACCOUNT_INSIGHTS_FIELDS)
                            .queryParam("time_range", timeRangeJson)
                            .queryParam("level", "account")
                            .build(apiVersion, normalizedAdAccountId))
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(MetaAdsInsightsResponseDTO.class);
        }catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao obter insights da conta de anuncio.", ex);
        }
    }

    private void validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new MetaGraphIntegrationException("Token da Meta Graph API nao configurado.");
        }
    }

    private String normalizeAdAccountId(String adAccountId) {
        if (!StringUtils.hasText(adAccountId)) {
            throw new IllegalArgumentException("Conta de anuncios e obrigatoria.");
        }

        String normalized = adAccountId.trim();
        if (normalized.startsWith("act_")) {
            normalized = normalized.substring("act_".length());
        }
        if (!normalized.matches("\\d+")) {
            throw new IllegalArgumentException("Conta de anuncios deve conter apenas digitos ou o prefixo act_.");
        }
        return normalized;
    }
}
