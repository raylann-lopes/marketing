package com.north.producoes.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.north.producoes.controller.api.WhatsAppWebhookApi;
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
import java.util.List;
import java.util.Set;

/**
 * Recebe eventos de mensagens da Evolution API via webhook.
 *
 * Estratégia de aprovação via enquete (poll):
 *   1. Sistema envia enquete com "✅ Aprovar" e "❌ Rejeitar"
 *   2. Cliente vota tocando na opção — sem digitar nada
 *   3. Webhook recebe "pollUpdateMessage" com o stanza ID da enquete
 *   4. Sistema busca a aprovação pelo stanza ID e processa o voto
 *
 * Vantagens: zero falsos positivos, sem matching de texto, UX simples.
 *
 * Configure na Evolution API:
 *   URL:     POST https://seudominio.com/api/webhooks/whatsapp/{EVOLUTION_WEBHOOK_SECRET}
 *   Eventos: messages.upsert
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController implements WhatsAppWebhookApi {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
            log.error("[Webhook] Falha ao deserializar: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }

        if (event == null || !"messages.upsert".equals(event.event())) {
            return ResponseEntity.ok().build();
        }

        EvolutionWebhookEventDTO.Data data = event.data();
        if (data == null || data.key() == null) {
            return ResponseEntity.ok().build();
        }

        // Ignora mensagens do próprio sistema
        if (Boolean.TRUE.equals(data.key().fromMe())) {
            return ResponseEntity.ok().build();
        }

        String messageType = data.messageType();
        log.info("[Webhook] messageType={} | remoteJid={}", messageType, data.key().remoteJid());

        // Processa voto de enquete (caminho principal)
        if ("pollUpdateMessage".equals(messageType)) {
            processPollVote(data);
            return ResponseEntity.ok().build();
        }

        // Fallback: resposta citando a mensagem (caso a enquete não funcione no ambiente)
        if ("extendedTextMessage".equals(messageType)) {
            String quotedStanzaId = extractQuotedStanzaId(data);
            if (StringUtils.hasText(quotedStanzaId)) {
                processFallbackReply(quotedStanzaId, data);
            }
        }

        return ResponseEntity.ok().build();
    }

    // ─── Processamento da enquete ──────────────────────────────────────────────

    private void processPollVote(EvolutionWebhookEventDTO.Data data) {
        if (data.message() == null || data.message().pollUpdateMessage() == null) {
            log.debug("[Webhook] pollUpdateMessage sem dados. Ignorando.");
            return;
        }

        EvolutionWebhookEventDTO.PollUpdate poll = data.message().pollUpdateMessage();

        if (poll.pollCreationMessageKey() == null) {
            log.warn("[Webhook] Poll sem pollCreationMessageKey. Ignorando.");
            return;
        }

        String pollStanzaId = poll.pollCreationMessageKey().id();
        if (!StringUtils.hasText(pollStanzaId)) {
            log.warn("[Webhook] Poll sem stanza ID da enquete original. Ignorando.");
            return;
        }

        // Busca a aprovação pelo stanza ID da enquete enviada pelo sistema
        ApproveEntity approval = approveRepository.findByWhatsappStanzaId(pollStanzaId).orElse(null);
        if (approval == null) {
            log.debug("[Webhook] Voto em enquete não relacionada ao sistema (stanza: {}). Ignorando.", pollStanzaId);
            return;
        }

        if (approval.getStatus() != ApproveStatusEnum.PENDING) {
            log.info("[Webhook] Aprovação ID={} já processada ({}). Ignorando voto.", approval.getId(), approval.getStatus());
            return;
        }

        // selectedOptions é List<String> — Evolution API envia strings diretas
        List<String> selected = poll.vote() != null ? poll.vote().selectedOptions() : List.of();

        if (selected == null || selected.isEmpty()) {
            log.info("[Webhook] Voto cancelado/removido na enquete. Ignorando.");
            return;
        }

        String votedOption = selected.getFirst();
        log.info("[Webhook] Voto recebido | aprovação ID={} | opção='{}'", approval.getId(), votedOption);

        if (WhatsAppNotificationService.OPTION_APPROVE.equals(votedOption)) {
            handleApprove(approval);
        } else if (WhatsAppNotificationService.OPTION_REJECT.equals(votedOption)) {
            handleReject(approval);
        } else {
            log.warn("[Webhook] Opção votada '{}' não reconhecida.", votedOption);
        }
    }

    // ─── Fallback: resposta citando a mensagem ─────────────────────────────────

    private void processFallbackReply(String quotedStanzaId, EvolutionWebhookEventDTO.Data data) {
        ApproveEntity approval = approveRepository.findByWhatsappStanzaId(quotedStanzaId).orElse(null);
        if (approval == null || approval.getStatus() != ApproveStatusEnum.PENDING) return;

        String text = extractText(data);
        if (!StringUtils.hasText(text)) return;

        String textLower = text.strip().toLowerCase();
        log.info("[Webhook] Fallback reply | aprovação ID={} | texto='{}'", approval.getId(), textLower);

        if (Set.of("sim", "yes", "aprovar", "aprovado", "👍").contains(textLower)) {
            handleApprove(approval);
        } else if (Set.of("não", "nao", "rejeitar", "rejeitado", "👎").contains(textLower)) {
            handleReject(approval);
        }
    }

    // ─── Ações ─────────────────────────────────────────────────────────────────

    private void handleApprove(ApproveEntity approval) {
        // O voto vem do grupo do WhatsApp do cliente, sem identificar a
        // pessoa — mostra o nome da empresa em vez de um placeholder genérico
        PostEntity post = approval.getPost();
        String approver = (post != null && post.getClient() != null)
                ? post.getClient().getName()
                : "whatsapp-client";

        approval.setStatus(ApproveStatusEnum.APPROVE);
        approval.setApprovedAt(LocalDateTime.now());
        approval.setApprovedUser(approver);
        approveRepository.save(approval);

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

    private void handleReject(ApproveEntity approval) {
        approval.setStatus(ApproveStatusEnum.REJECTED);
        approval.setApprovedAt(null);
        approval.setApprovedUser("");
        approveRepository.save(approval);

        PostEntity post = approval.getPost();
        if (post != null) {
            post.setStatus(PostStatusEnum.REJECTED);
            postRepository.save(post);
            log.info("[Webhook] ❌ Post ID {} REJEITADO → status REJECTED.", post.getId());
            whatsAppNotificationService.sendRejectionNotification(
                    post.getClient().getId(), post.getId(), null);
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private String extractQuotedStanzaId(EvolutionWebhookEventDTO.Data data) {
        if (data.message() == null || data.message().extendedTextMessage() == null) return null;
        var ctx = data.message().extendedTextMessage().contextInfo();
        return ctx != null ? ctx.stanzaId() : null;
    }

    private String extractText(EvolutionWebhookEventDTO.Data data) {
        if (data.message() == null) return null;
        if (StringUtils.hasText(data.message().conversation())) return data.message().conversation();
        if (data.message().extendedTextMessage() != null) return data.message().extendedTextMessage().text();
        return null;
    }
}
