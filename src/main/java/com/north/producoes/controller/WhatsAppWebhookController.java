package com.north.producoes.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Recebe eventos de mensagens da Evolution API via webhook.
 *
 * Configure na Evolution API:
 *   URL:     POST https://SEU-NGROK.ngrok.io/api/webhooks/whatsapp/{EVOLUTION_WEBHOOK_SECRET}
 *   Eventos: messages.upsert
 *
 * EVOLUTION_WEBHOOK_SECRET deve estar definido no .env local e no compose.yaml.
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    // ObjectMapper próprio — não depende de bean Spring para evitar falha de contexto
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Set<String> APPROVE_KEYWORDS = Set.of(
            "sim", "s", "yes", "aprovar", "aprovado", "ok", "👍", "confirmar"
    );
    private static final Set<String> REJECT_KEYWORDS = Set.of(
            "não", "nao", "n", "no", "rejeitar", "rejeitado", "cancelar", "👎"
    );

    // @Value injetado por setter — não entra no construtor do @RequiredArgsConstructor
    @Value("${evolution.webhook.secret:}")
    private String webhookSecret;

    // Dependências Spring — final → incluídas no construtor gerado pelo @RequiredArgsConstructor
    private final ApproveRepository approveRepository;
    private final PostRepository postRepository;
    private final WhatsAppNotificationService whatsAppNotificationService;

    /**
     * Formato padrão: byEvents=false → Evolution API chama /{secret}
     * Formato byEvents=true → Evolution API chama /{secret}/messages-upsert
     * Ambos os formatos são suportados.
     */
    @PostMapping({"/{secret}", "/{secret}/messages-upsert"})
    @Transactional
    public ResponseEntity<Void> handleWebhook(
            @PathVariable String secret,
            @RequestBody String rawBody) {

        // 1. Valida secret
        if (!StringUtils.hasText(webhookSecret)) {
            log.error("[Webhook] EVOLUTION_WEBHOOK_SECRET não está configurado! " +
                      "Defina a variável de ambiente e reinicie a aplicação.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!webhookSecret.equals(secret)) {
            log.warn("[Webhook] Secret inválido. Verifique a URL configurada na Evolution API.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 2. Loga payload cru para diagnóstico
        log.info("[Webhook] ← Payload: {}", rawBody);

        // 3. Deserializa
        EvolutionWebhookEventDTO event;
        try {
            event = MAPPER.readValue(rawBody, EvolutionWebhookEventDTO.class);
        } catch (Exception e) {
            log.error("[Webhook] Falha ao deserializar payload: {}", e.getMessage());
            return ResponseEntity.ok().build();
        }

        // 4. Filtra evento
        if (event == null || !"messages.upsert".equals(event.event())) {
            log.info("[Webhook] Evento ignorado: {}", event != null ? event.event() : "null");
            return ResponseEntity.ok().build();
        }

        EvolutionWebhookEventDTO.Data data = event.data();
        if (data == null || data.key() == null) {
            log.warn("[Webhook] data ou data.key é null.");
            return ResponseEntity.ok().build();
        }

        // 5. Ignora mensagens próprias do sistema
        if (Boolean.TRUE.equals(data.key().fromMe())) {
            log.debug("[Webhook] Mensagem própria ignorada (fromMe=true)");
            return ResponseEntity.ok().build();
        }

        String remoteJid = data.key().remoteJid();
        log.info("[Webhook] remoteJid={} | messageType={}", remoteJid, data.messageType());

        // 6. Somente grupos
        if (!StringUtils.hasText(remoteJid) || !remoteJid.contains("@g.us")) {
            log.info("[Webhook] remoteJid '{}' não é grupo (@g.us). Ignorando.", remoteJid);
            return ResponseEntity.ok().build();
        }

        // 7. Extrai texto
        String text = extractText(data);
        log.info("[Webhook] Texto extraído: '{}'", text);

        if (!StringUtils.hasText(text)) {
            log.info("[Webhook] Mensagem sem texto (mídia, sticker, etc.). Ignorando.");
            return ResponseEntity.ok().build();
        }

        processGroupResponse(remoteJid, text.trim());
        return ResponseEntity.ok().build();
    }

    private void processGroupResponse(String remoteJid, String rawText) {
        String textLower = rawText.toLowerCase().strip();
        String cleanGroupId     = remoteJid.replace("@g.us", "");
        String groupIdWithSuffix = cleanGroupId + "@g.us";

        log.info("[Webhook] Buscando aprovação PENDING | groupId='{}' | texto='{}'",
                cleanGroupId, textLower);

        List<ApproveEntity> approvals = approveRepository
                .findByStatusAndClientWhatsappGroupId(
                        ApproveStatusEnum.PENDING, cleanGroupId, groupIdWithSuffix);

        if (approvals.isEmpty()) {
            log.warn("[Webhook] Nenhuma aprovação PENDING para grupo '{}'. " +
                     "Verifique: (1) tb_client.whatsapp_group_id está preenchido, " +
                     "(2) existe tb_post_approvals com status=PENDING para este cliente.",
                    remoteJid);
            return;
        }

        ApproveEntity approval = approvals.getFirst();
        log.info("[Webhook] Aprovação ID={} encontrada | Post ID={}",
                approval.getId(),
                approval.getPost() != null ? approval.getPost().getId() : "null");

        if (APPROVE_KEYWORDS.contains(textLower)) {
            log.info("[Webhook] Palavra-chave de APROVAÇÃO: '{}'", rawText);
            handleApprove(approval, rawText);
        } else if (REJECT_KEYWORDS.contains(textLower)) {
            log.info("[Webhook] Palavra-chave de REJEIÇÃO: '{}'", rawText);
            handleReject(approval, rawText);
        } else {
            log.warn("[Webhook] Texto '{}' não reconhecido. " +
                     "Aprovar com: {} | Rejeitar com: {}",
                    rawText, APPROVE_KEYWORDS, REJECT_KEYWORDS);
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

        // Se a data agendada já passou, reagenda para HOJE no mesmo horário
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = post.getScheduledAt();

        if (scheduledAt != null && scheduledAt.toLocalDate().isBefore(now.toLocalDate())) {
            // Mantém o horário original, mas usa o dia de hoje
            LocalDateTime todayAtSameTime = now
                    .withHour(scheduledAt.getHour())
                    .withMinute(scheduledAt.getMinute())
                    .withSecond(0)
                    .withNano(0);

            // Se o horário de hoje também já passou → publica na próxima rodada do scheduler (+ 1 min)
            if (todayAtSameTime.isBefore(now)) {
                todayAtSameTime = now.plusMinutes(1);
            }

            log.info("[Webhook] Data original {} já passou. Reagendando para hoje: {}",
                    scheduledAt, todayAtSameTime);
            post.setScheduledAt(todayAtSameTime);
        }

        post.setStatus(PostStatusEnum.SCHEDULE);
        postRepository.save(post);
        log.info("[Webhook] ✅ Post ID {} APROVADO → SCHEDULE | publicação em: {}",
                post.getId(), post.getScheduledAt());
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
            whatsAppNotificationService.sendRejectionNotification(
                    post.getClient(), post, responseText);
        }
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
