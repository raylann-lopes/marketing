package com.north.producoes.integration.evolutionApi;

import com.north.producoes.exception.EvolutionApiIntegrationException;
import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSendMediaDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSendTextDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSentMessageDTO;
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
        validate();
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

    /**
     * Envia mensagem de texto para um grupo WhatsApp.
     *
     * @param groupId ID do grupo (ex.: 120363...@g.us)
     * @param text    Conteúdo da mensagem
     * @return stanza ID da mensagem enviada
     */
    public String sendTextToGroup(String groupId, String text) {
        validate();
        try {
            EvolutionSentMessageDTO response = evolutionApiRestClient.post()
                    .uri("/message/sendText/{instance}", instance)
                    .header("apikey", apiKey)
                    .body(new EvolutionSendTextDTO(groupId, text))
                    .retrieve()
                    .body(EvolutionSentMessageDTO.class);

            return response != null && response.key() != null ? response.key().id() : null;
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao enviar mensagem de texto na Evolution API.", ex);
        }
    }

    /**
     * Envia imagem com legenda para um grupo WhatsApp.
     *
     * @param groupId  ID do grupo (ex.: 120363...@g.us)
     * @param mediaUrl URL pública da imagem
     * @param caption  Texto acompanhando a imagem
     * @return stanza ID da mensagem enviada
     */
    public String sendMediaToGroup(String groupId, String mediaUrl, String caption) {
        validate();
        try {
            EvolutionSentMessageDTO response = evolutionApiRestClient.post()
                    .uri("/message/sendMedia/{instance}", instance)
                    .header("apikey", apiKey)
                    .body(new EvolutionSendMediaDTO(groupId, "image", mediaUrl, caption))
                    .retrieve()
                    .body(EvolutionSentMessageDTO.class);

            return response != null && response.key() != null ? response.key().id() : null;
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao enviar mídia na Evolution API.", ex);
        }
    }

    private void validate() {
        if (!StringUtils.hasText(apiKey)) {
            throw new EvolutionApiIntegrationException("API key para Evolution API nao configurada.");
        }
        if (!StringUtils.hasText(instance)) {
            throw new EvolutionApiIntegrationException("Instancia para Evolution API nao configurada.");
        }
    }
}
