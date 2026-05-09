package com.north.producoes.integration.apify;

import com.north.producoes.exception.ApifyIntegrationException;
import com.north.producoes.integration.apify.dto.ApifyInstagramPostResponseDTO;
import com.north.producoes.integration.apify.dto.ApifyInstagramRunRequestDTO;
import com.north.producoes.integration.apify.dto.ApifyRunResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApifyClient {
    private final RestClient apifyRestClient;

    @Value("${apify.api.token:}")
    private String apiKey;

    @Value("${apify.instagram.actor-id:apify/instagram-api-scraper}")
    private String actor;

    @Value("${apify.instagram.max-results:20}")
    private Integer maxResults;

    public ApifyRunResponseDTO runInstagramScraper(ApifyInstagramRunRequestDTO request) {
        validateConfig();

        if (request == null || request.search() == null || request.search().isEmpty()) {
            throw new ApifyIntegrationException("Termos de busca para Apify sao obrigatorios.");
        }

        try {
            return apifyRestClient.post()
                    .uri("/v2/acts/{actor}/runs", normalizedActor())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ApifyRunResponseDTO.class);
        } catch (RestClientException ex) {
            throw new ApifyIntegrationException("Falha ao iniciar execucao do Actor na Apify.", ex);
        }
    }

    public ApifyRunResponseDTO getRun(String runId) {
        validateConfig();
        if (!StringUtils.hasText(runId)) {
            throw new ApifyIntegrationException("Run ID da Apify e obrigatorio.");
        }

        try {
            return apifyRestClient.get()
                    .uri("/v2/actor-runs/{runId}", runId)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(ApifyRunResponseDTO.class);
        } catch (RestClientException ex) {
            throw new ApifyIntegrationException("Falha ao consultar status da execucao na Apify.", ex);
        }
    }

    public List<ApifyInstagramPostResponseDTO> getDatasetItems(String datasetId) {
        return getDatasetItems(datasetId, maxResults);
    }

    public List<ApifyInstagramPostResponseDTO> getDatasetItems(String datasetId, Integer limit) {
        validateConfig();
        if (!StringUtils.hasText(datasetId)) {
            throw new ApifyIntegrationException("Dataset ID da Apify e obrigatorio.");
        }

        int safeLimit = resolveLimit(limit);

        try {
            ApifyInstagramPostResponseDTO[] response = apifyRestClient.get()
                    .uri("/v2/datasets/{datasetId}/items?clean=true&format=json&limit={limit}", datasetId, safeLimit)
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(ApifyInstagramPostResponseDTO[].class);

            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new ApifyIntegrationException("Falha ao consultar itens do dataset na Apify.", ex);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(apiKey)) {
            throw new ApifyIntegrationException("API key para Apify nao configurada.");
        }

        if (!StringUtils.hasText(actor)) {
            throw new ApifyIntegrationException("Actor ID para Apify nao configurado.");
        }
    }

    private String normalizedActor() {
        return actor.trim().replace("/", "~");
    }

    private int resolveLimit(Integer requestedLimit) {
        int configuredMax = maxResults != null && maxResults > 0 ? maxResults : 100;
        if (requestedLimit == null || requestedLimit <= 0) {
            return configuredMax;
        }
        return Math.min(requestedLimit, configuredMax);
    }
}
