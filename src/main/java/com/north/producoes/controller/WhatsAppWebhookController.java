package com.north.producoes.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.integration.evolutionApi.dto.EvolutionWebhookEventDTO;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Recebe eventos de mensagens da Evolution API via webhook.
 *
 * Estratégia de matching: processa SOMENTE mensagens que citam (respondem)
 * a mensagem de aprovação enviada pelo sistema. Isso elimina falsos positivos
 * de conversas normais no grupo — "ok", "sim" em outro contexto são ignorados.
 *
 * Configure na Evolution API:
 *   URL:     POST https://seudominio.com/api/webhooks/whatsapp/{EVOLUTION_WEBHOOK_SECRET}
 *   Eventos: messages.upsert
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Set<String> APPROVE_KEYWORDS = Set.of(
            "sim", "yes", "aprovar", "aprovado", "👍", "confirmar"
    );
    private static final Set<String> REJECT_KEYWORDS = Set.of(
            "não", "nao", "rejeitar", "rejeitado", "cancelar", "👎"
    );

    @Value("${evolution.webhook.secret:}")
    private String webhookSecret;

    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    @PostMapping({"/{secret}", "/{secret}/messages-upsert"})
    @Transactional
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String secret,
            @RequestBody String rawBody) {

        if (!StringUtils.hasText(webhookSecret)) {
            log.error("[Webhook] EVOLUTION_WEBHOOK_SECRET não configurado.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        if (!MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8))) {
            log.warn("[Webhook] Secret inválido.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        log.info("[Webhook] ← Requisição recebida | {} bytes", rawBody.length());

        EvolutionWebhookEventDTO event;
        try {
            event = MAPPER.readValue(rawBody, EvolutionWebhookEventDTO.class);
        } catch (Exception e) {
            log.error("[Webhook] Falha ao deserializar payload: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }

        if (event == null || !"messages.upsert".equals(event.event())) {
            return ResponseEntity.ok().build();
        }

        EvolutionWebhookEventDTO.Data data = event.data();
        if (data == null || data.key() == null) {
            return ResponseEntity.ok().build();
        }

        // Ignora mensagens enviadas pelo próprio sistema
        if (Boolean.TRUE.equals(data.key().fromMe())) {
            return ResponseEntity.ok().build();
        }

        // Somente grupos
        String remoteJid = data.key().remoteJid();
        if (!StringUtils.hasText(remoteJid) || !remoteJid.contains("@g.us")) {
            return ResponseEntity.ok().build();
        }

        // Tenta processar via stanza ID (resposta citando a mensagem de aprovação)
        // Se não for uma resposta citada, ignora — evita falsos positivos de conversas normais
        String quotedStanzaId = extractQuotedStanzaId(data);
        if (StringUtils.hasText(quotedStanzaId)) {
            processQuotedReply(quotedStanzaId, data);
        } else {
            log.debug("[Webhook] Mensagem sem citação — conversa normal do grupo. Ignorando.");
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Processa apenas respostas que citam a mensagem de aprovação enviada pelo sistema.
     * Busca a aprovação pelo stanzaId da mensagem original — matching preciso, sem ambiguidade.
     */
    private void processQuotedReply(String quotedStanzaId, EvolutionWebhookEventDTO.Data data) {
        ApproveEntity approval = approveRepository
                .findByWhatsappStanzaId(quotedStanzaId)
                .orElse(null);

        if (approval == null) {
            log.debug("[Webhook] Resposta citando stanza '{}' — não é mensagem de aprovação do sistema.",
                    quotedStanzaId);
            return;
        }

        if (approval.getStatus() != ApproveStatusEnum.PENDING) {
            log.info("[Webhook] Aprovação ID={} já foi processada (status: {}). Ignorando.",
                    approval.getId(), approval.getStatus());
            return;
        }

        String text = extractText(data);
        if (!StringUtils.hasText(text)) {
            log.info("[Webhook] Resposta citada sem texto. Ignorando.");
            return;
        }

        String textLower = text.strip().toLowerCase();
        log.info("[Webhook] Resposta à aprovação ID={} | texto='{}'", approval.getId(), textLower);

        if (APPROVE_KEYWORDS.contains(textLower)) {
            handleApprove(approval, text.strip());
        } else if (REJECT_KEYWORDS.contains(textLower)) {
            handleReject(approval, text.strip());
        } else {
            log.warn("[Webhook] Resposta '{}' não reconhecida para aprovação ID={}. " +
                     "Peça ao cliente responder com: {} (aprovar) ou {} (rejeitar).",
                    text, approval.getId(), APPROVE_KEYWORDS, REJECT_KEYWORDS);
        }
    }

    private void handleApprove(ApproveEntity approval, String responseText) {
        approval.setStatus(ApproveStatusEnum.APPROVE);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprovedUser("whatsapp-client");
        approval.setWhatsappResponseText(responseText);
        approveRepository.save(approval);

        PostEntity post = approval.getPost();
        if (post == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = post.getScheduledAt();

        if (scheduledAt != null && scheduledAt.toLocalDate().isBefore(now.toLocalDate())) {
            LocalDateTime todayAtSameTime = now
                    .withHour(scheduledAt.getHour())
                    .withMinute(scheduledAt.getMinute())
                    .withSecond(0).withNano(0);

            if (todayAtSameTime.isBefore(now)) {
                todayAtSameTime = now.plusMinutes(1);
            }

            log.info("[Webhook] Data original {} ultrapassada. Reagendando para: {}", scheduledAt, todayAtSameTime);
            post.setScheduledAt(todayAtSameTime);
        }

        post.setStatus(PostStatusEnum.SCHEDULE);
        postRepository.save(post);
        log.info("[Webhook] ✅ Post ID {} APROVADO → SCHEDULE | publicação: {}", post.getId(), post.getScheduledAt());
    }

    private void handleReject(ApproveEntity approval, String responseText) {
        approval.setStatus(ApproveStatusEnum.REJECTED);
        approval.setApprovedAt(null);
        approval.setApprovedUser("");
        approval.setWhatsappResponseText(responseText);
        approveRepository.save(approval);

        PostEntity post = approval.getPost();
        if (post != null) {
            log.info("[Webhook] ❌ Post ID {} REJEITADO. Notificando grupo.", post.getId());
            whatsAppNotificationService.sendRejectionNotification(post.getClient(), post, responseText);
        }
    }

    /**
     * Extrai o stanzaId da mensagem que foi citada (caso seja uma resposta/reply).
     * Retorna null se a mensagem não citar nada — conversa normal do grupo.
     */
    private String extractQuotedStanzaId(EvolutionWebhookEventDTO.Data data) {
        if (data.message() == null) return null;

        // Mensagem de resposta/citação tem contextInfo com stanzaId da mensagem original
        if (data.message().extendedTextMessage() != null
                && data.message().extendedTextMessage().contextInfo() != null) {
            return data.message().extendedTextMessage().contextInfo().stanzaId();
        }

        return null;
    }

    private String extractText(EvolutionWebhookEventDTO.Data data) {
        if (data.message() == null) return null;

        if (StringUtils.hasText(data.message().conversation())) {
            return data.message().conversation();
        }

        if (data.message().extendedTextMessage() != null
                && StringUtils.hasText(data.message().extendedTextMessage().text())) {
            return data.message().extendedTextMessage().text();
        }

        return null;
    }
}
