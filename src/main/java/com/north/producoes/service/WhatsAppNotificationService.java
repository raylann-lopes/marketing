package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.repository.ApproveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Envia notificações WhatsApp diretamente via Evolution API.
 * Todos os métodos são @Async — não bloqueiam o fluxo principal.
 *
 * O groupId é SEMPRE obtido do cliente do post — nunca de parâmetro externo,
 * garantindo que a mensagem vá para o grupo correto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    private final EvolutionApiClient evolutionApiClient;
    private final ApproveRepository approveRepository;

    /**
     * Envia imagem + mensagem de aprovação para o grupo WhatsApp do cliente.
     * Registra o stanza ID para rastrear a resposta.
     */
    @Async
    public void sendApprovalRequest(ClientEntity client, PostEntity post,
                                    ApproveEntity approve, String mediaUrl) {
        String groupId = client.getWhatsappGroupId();
        if (!StringUtils.hasText(groupId)) {
            log.warn("[WhatsApp] Cliente '{}' sem grupo vinculado. Aprovação não enviada.", client.getName());
            return;
        }

        String caption = buildApprovalMessage(post, approve);

        try {
            String stanzaId = evolutionApiClient.sendMediaToGroup(groupId, mediaUrl, caption);
            saveStanzaId(approve, stanzaId);
            log.info("[WhatsApp] Aprovação enviada para grupo '{}' | Post ID: {} | Stanza: {}",
                    client.getWhatsappGroupName(), post.getId(), stanzaId);
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao enviar aprovação para cliente '{}': {}",
                    client.getName(), e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de rejeição de post para o grupo WhatsApp do cliente.
     */
    @Async
    public void sendRejectionNotification(ClientEntity client, PostEntity post, String rejectionReason) {
        String groupId = client.getWhatsappGroupId();
        if (!StringUtils.hasText(groupId)) {
            log.warn("[WhatsApp] Cliente '{}' sem grupo vinculado. Rejeição não notificada.", client.getName());
            return;
        }

        String message = buildRejectionMessage(post, rejectionReason);

        try {
            evolutionApiClient.sendTextToGroup(groupId, message);
            log.info("[WhatsApp] Rejeição notificada para grupo '{}' | Post ID: {}",
                    client.getWhatsappGroupName(), post.getId());
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao notificar rejeição para cliente '{}': {}",
                    client.getName(), e.getMessage(), e);
        }
    }

    private void saveStanzaId(ApproveEntity approve, String stanzaId) {
        if (!StringUtils.hasText(stanzaId) || approve.getId() == null) return;
        approve.setWhatsappStanzaId(stanzaId);
        approve.setWhatsappSentAt(LocalDateTime.now().toString());
        approveRepository.save(approve);
    }

    private String buildApprovalMessage(PostEntity post, ApproveEntity approve) {
        StringBuilder sb = new StringBuilder();
        sb.append("📸 *Aprovação de Arte*\n\n");
        sb.append("*Post:* ").append(post.getTitle()).append("\n");
        sb.append("*Temática:* ").append(post.getTheme()).append("\n");
        sb.append("*Agendado para:* ").append(formatDate(post.getScheduledAt())).append("\n");

        if (StringUtils.hasText(approve.getCaption())) {
            sb.append("\n*Legenda sugerida:*\n").append(approve.getCaption()).append("\n");
        }

        sb.append("\n↩️ *Responda ESTA mensagem* com:\n")
          .append("✅ *SIM* para aprovar\n")
          .append("❌ *NÃO* para rejeitar\n\n")
          .append("_⚠️ É necessário citar esta mensagem para que o sistema reconheça sua resposta._");
        return sb.toString();
    }

    private String buildRejectionMessage(PostEntity post, String rejectionReason) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌ *Arte Rejeitada*\n\n");
        sb.append("*Post:* ").append(post.getTitle()).append("\n");
        if (StringUtils.hasText(rejectionReason)) {
            sb.append("*Motivo:* ").append(rejectionReason).append("\n");
        }
        sb.append("\nNosso time irá revisar e enviar uma nova versão em breve. 🙏");
        return sb.toString();
    }

    private String formatDate(LocalDateTime dt) {
        if (dt == null) return "a definir";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
    }
}
