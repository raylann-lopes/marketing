package com.north.producoes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class N8nWebhookService {
    private static final Logger log = LoggerFactory.getLogger(N8nWebhookService.class);
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;

    @Value("${n8n.webhook.upload-complete-url:}")
    private String uploadCompleteUrl;

    @Value("${n8n.webhook.publish-url:}")
    private String publishUrl;

    @Value("${n8n.webhook.api-key:}")
    private String webhookApiKey;

    public N8nWebhookService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Async
    public void dispatchArtUploadCompleted(Map<String, Object> payload) {
        dispatch(uploadCompleteUrl, payload);
    }

    @Async
    public void dispatchPublishPost(Map<String, Object> payload) {
        dispatch(publishUrl, payload);
    }

    @Async
    public void dispatchPostRejected(Map<String, Object> payload) {
        dispatch(uploadCompleteUrl, payload);
    }

    private void dispatch(String url, Map<String, Object> payload) {
        if (!StringUtils.hasText(url)) {
            log.warn("Webhook n8n não configurado. Payload não enviado.");
            return;
        }

        if (!StringUtils.hasText(webhookApiKey)) {
            log.error("Webhook n8n bloqueado: n8n.webhook.api-key não configurado.");
            return;
        }

        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, webhookApiKey);

            request.body(payload).retrieve().toBodilessEntity();
            log.info("Webhook enviado com sucesso para: {}", url);
        } catch (Exception ex) {
            log.error("Falha ao disparar webhook n8n em {} às {}: {}", url, LocalDateTime.now(), ex.getMessage());
        }
    }
}
