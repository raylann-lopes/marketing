package com.north.producoes.integration.meta;

import com.north.producoes.controller.dto.response.MetaAccountsResponseDTO;
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
}
