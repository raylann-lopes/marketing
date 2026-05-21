package com.north.producoes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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

    public boolean dispatchArtUploadCompleted(Map<String, Object> payload) {
        return dispatch(uploadCompleteUrl, payload);
    }

    public boolean dispatchPublishPost(Map<String, Object> payload) {
        return dispatch(publishUrl, payload);
    }

    private boolean dispatch(String url, Map<String, Object> payload) {
        if (!StringUtils.hasText(url)) {
            log.warn("Webhook n8n não configurado para URL: {}. Payload não enviado.", url);
            return false;
        }

        if (!StringUtils.hasText(webhookApiKey)) {
            log.error("Webhook n8n bloqueado: n8n.webhook.api-key não configurado.");
            return false;
        }

        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(API_KEY_HEADER, webhookApiKey);

            request.body(payload).retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.error("Falha ao disparar webhook n8n em {} às {}: {}", url, LocalDateTime.now(), ex.getMessage(), ex);
            return false;
        }
    }
}
