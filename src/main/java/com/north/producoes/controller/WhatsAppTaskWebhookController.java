package com.north.producoes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import com.north.producoes.service.WhatsAppTaskInboxService;
import com.north.producoes.service.WhatsAppTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/webhooks/whatsapp/tasks", "/api/webhooks/whatsapp/task"})
public class WhatsAppTaskWebhookController {

    private final ObjectMapper objectMapper;
    private final WhatsAppTaskInboxService inboxService;
    private final WhatsAppTaskService whatsAppTaskService;

    @Value("${evolution.webhook.tasks-secret:}")
    private String webhookSecret;

    @PostMapping({"/{secret}", "/{secret}/messages-upsert"})
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String secret,
            @RequestBody String rawBody) {

        if (!StringUtils.hasText(webhookSecret)) {
            log.error("[TaskWebhook] EVOLUTION_TASKS_WEBHOOK_SECRET não configurado.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (!MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("[TaskWebhook] Secret inválido.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            EvolutionWebhookEventDTO event = objectMapper.readValue(rawBody, EvolutionWebhookEventDTO.class);
            if (event != null && event.data() != null && event.data().key() != null) {
                log.info("[TaskWebhook] Evento recebido | type={} | messageId={}",
                        event.data().messageType(), event.data().key().id());
            }
            inboxService.enqueue(rawBody, event)
                    .ifPresent(whatsAppTaskService::processStoredEvent);
        } catch (JsonProcessingException ex) {
            // O webhook é confirmado para evitar reenvios indefinidos de um payload inválido.
            log.error("[TaskWebhook] Payload inválido: {}", ex.getMessage(), ex);
        }

        return ResponseEntity.ok().build();
    }
}
