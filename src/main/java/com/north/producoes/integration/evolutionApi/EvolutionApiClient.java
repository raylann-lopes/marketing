package com.north.producoes.integration.evolutionApi;

import com.north.producoes.exception.EvolutionApiIntegrationException;
import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;
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
public class EvolutionApiClient {
    private final RestClient evolutionApiRestClient;

    @Value("${evolution.api.key:}")
    private String apiKey;

    @Value("${evolution.api.instance:}")
    private String instance;

    public List<EvolutionGroupApiResponseDTO> findGroups() {
        if (!StringUtils.hasText(apiKey)) {
            throw new EvolutionApiIntegrationException("API key para Evolution API nao configurada.");
        }

        if (!StringUtils.hasText(instance)) {
            throw new EvolutionApiIntegrationException("Instancia para Evolution API nao configurada.");
        }

        try {
            EvolutionGroupApiResponseDTO[] response = evolutionApiRestClient.get()
                    .uri("/group/fetchAllGroups/{instance}?getParticipants=false", instance)
                    .header("apikey", apiKey)
                    .retrieve()
                    .body(EvolutionGroupApiResponseDTO[].class);

            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao consultar grupos na Evolution API.", ex);
        }
    }
}
