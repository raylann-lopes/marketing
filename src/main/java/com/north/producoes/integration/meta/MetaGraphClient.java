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

@Component
@RequiredArgsConstructor
public class MetaGraphClient {
    private static final String PAGE_FIELDS = "id,name,access_token,instagram_business_account{id,username,name}";

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
                    .uri("/{apiVersion}/me/accounts?fields={fields}&access_token={accessToken}",
                            apiVersion,
                            PAGE_FIELDS,
                            accessToken)
                    .retrieve()
                    .body(MetaAccountsResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao consultar contas na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO createContainer(String igUserId, String imageUrl, String caption, String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new MetaGraphIntegrationException("Token da Meta Graph API nao configurado.");
        }

        MetaContainerRequestDTO request = new MetaContainerRequestDTO(imageUrl, caption, accessToken);

        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media?access_token={accessToken}",
                            apiVersion,
                            igUserId,
                            accessToken)
                    .body(request)
                    .retrieve()
                    .body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao criar container na Meta Graph API.", ex);
        }
    }

    public MetaContainerStatusResponseDTO checkContainerStatus(String igContainerId, String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new MetaGraphIntegrationException("Token da Meta Graph API nao configurado");
        }

        try {
            return metaGraphRestClient.get()
                    .uri("/{apiVersion}/{igContainerId}?fields=status_code&access_token={accessToken}",
                            apiVersion,
                            igContainerId,
                            accessToken)
                    .retrieve()
                    .body(MetaContainerStatusResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao consultar status do container na Meta Graph API.", ex);
        }
    }

    public MetaContainerIdResponseDTO publish(String igUserId, String creationId, String accessToken){
        if (!StringUtils.hasText(accessToken)) {
            throw new MetaGraphIntegrationException("Token da Meta Graph API nao configurado");
        }

        MetaPublishRequestDTO request = new MetaPublishRequestDTO(creationId, accessToken);

        try {
            return metaGraphRestClient.post()
                    .uri("/{apiVersion}/{igUserId}/media_publish?access_token={accessToken}",
                            apiVersion,
                            igUserId,
                            accessToken)
                    .body(request)
                    .retrieve()
                    .body(MetaContainerIdResponseDTO.class);
        } catch (RestClientException ex) {
            throw new MetaGraphIntegrationException("Falha ao publicar container na Meta Graph API.", ex);
        }
    }
}
