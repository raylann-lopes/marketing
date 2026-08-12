package com.north.producoes.integration.evolutionApi;

import com.north.producoes.exception.EvolutionApiIntegrationException;
import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSendMediaDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSendPollDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSendTextDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionSentMessageDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionMediaDownloadRequestDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionMediaDownloadResponseDTO;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

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
        return sendText(groupId, instance, text);
    }

    public String sendText(String recipient, String targetInstance, String text) {
        return sendText(recipient, targetInstance, apiKey, text);
    }

    public String sendText(String recipient, String targetInstance, String targetApiKey, String text) {
        validate(targetInstance, targetApiKey);
        try {
            EvolutionSentMessageDTO response = evolutionApiRestClient.post()
                    .uri("/message/sendText/{instance}", targetInstance)
                    .header("apikey", targetApiKey)
                    .body(new EvolutionSendTextDTO(recipient, text))
                    .retrieve()
                    .body(EvolutionSentMessageDTO.class);

            return response != null && response.key() != null ? response.key().id() : null;
        } catch (RestClientResponseException ex) {
            throw evolutionError("enviar mensagem de texto", ex);
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao enviar mensagem de texto na Evolution API.", ex);
        }
    }

    public EvolutionMediaDownloadResponseDTO downloadMedia(
            EvolutionWebhookEventDTO.Key messageKey,
            String targetInstance,
            String targetApiKey) {
        validate(targetInstance, targetApiKey);
        try {
            return evolutionApiRestClient.post()
                    .uri("/chat/getBase64FromMediaMessage/{instance}", targetInstance)
                    .header("apikey", targetApiKey)
                    .body(EvolutionMediaDownloadRequestDTO.from(messageKey))
                    .retrieve()
                    .body(EvolutionMediaDownloadResponseDTO.class);
        } catch (RestClientResponseException ex) {
            throw evolutionError("baixar mídia", ex);
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao baixar mídia na Evolution API.", ex);
        }
    }

    /**
     * Envia imagem ou vídeo com legenda para um grupo WhatsApp.
     * O mediatype é inferido pela extensão da URL — um vídeo declarado como
     * "image" falha na renderização do WhatsApp.
     *
     * @param groupId  ID do grupo (ex.: 120363...@g.us)
     * @param mediaUrl URL pública da mídia
     * @param caption  Texto acompanhando a mídia
     * @return stanza ID da mensagem enviada
     */
    public String sendMediaToGroup(String groupId, String mediaUrl, String caption) {
        validate();
        try {
            String mediatype = isVideoUrl(mediaUrl) ? "video" : "image";
            EvolutionSentMessageDTO response = evolutionApiRestClient.post()
                    .uri("/message/sendMedia/{instance}", instance)
                    .header("apikey", apiKey)
                    .body(new EvolutionSendMediaDTO(groupId, mediatype, mediaUrl, caption))
                    .retrieve()
                    .body(EvolutionSentMessageDTO.class);

            return response != null && response.key() != null ? response.key().id() : null;
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao enviar mídia na Evolution API.", ex);
        }
    }

    private boolean isVideoUrl(String url) {
        if (!StringUtils.hasText(url)) return false;
        String clean = url.split("\\?")[0].toLowerCase();
        return clean.endsWith(".mp4") || clean.endsWith(".mov")
                || clean.endsWith(".webm") || clean.endsWith(".avi") || clean.endsWith(".mkv");
    }

    /**
     * Envia uma enquete (poll) para um grupo WhatsApp.
     * O cliente vota tocando em uma opção — não precisa digitar nada.
     *
     * @param groupId         ID do grupo (ex.: 120363...@g.us)
     * @param question        Título/pergunta da enquete
     * @param options         Opções disponíveis (máx. recomendado: 12)
     * @param selectableCount Quantidade de opções que o usuário pode selecionar (1 = escolha única)
     * @return stanza ID da enquete enviada (salvar para matching no webhook)
     */
    public String sendPollToGroup(String groupId, String question, List<String> options, int selectableCount) {
        validate();
        try {
            EvolutionSentMessageDTO response = evolutionApiRestClient.post()
                    .uri("/message/sendPoll/{instance}", instance)
                    .header("apikey", apiKey)
                    .body(new EvolutionSendPollDTO(groupId, question, selectableCount, options))
                    .retrieve()
                    .body(EvolutionSentMessageDTO.class);

            return response != null && response.key() != null ? response.key().id() : null;
        } catch (RestClientException ex) {
            throw new EvolutionApiIntegrationException("Falha ao enviar enquete na Evolution API.", ex);
        }
    }

    private void validate() {
        validate(instance, apiKey);
    }

    private void validate(String targetInstance) {
        validate(targetInstance, apiKey);
    }

    private void validate(String targetInstance, String targetApiKey) {
        if (!StringUtils.hasText(targetApiKey)) {
            throw new EvolutionApiIntegrationException("API key para Evolution API nao configurada.");
        }
        if (!StringUtils.hasText(targetInstance)) {
            throw new EvolutionApiIntegrationException("Instancia para Evolution API nao configurada.");
        }
    }

    private EvolutionApiIntegrationException evolutionError(String operation, RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        String normalizedBody = StringUtils.hasText(body) ? body.replaceAll("[\\r\\n]+", " ") : "";
        String safeBody = StringUtils.hasText(normalizedBody)
                ? normalizedBody.substring(0, Math.min(normalizedBody.length(), 300))
                : "sem detalhes";
        return new EvolutionApiIntegrationException(
                "Falha ao %s na Evolution API (HTTP %d): %s"
                        .formatted(operation, ex.getStatusCode().value(), safeBody),
                ex
        );
    }
}
