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
import java.util.List;

/**
 * Envia notificações WhatsApp diretamente via Evolution API.
 * Todos os métodos públicos são @Async — não bloqueiam o fluxo principal.
 *
 * Fluxo de aprovação:
 *   1. Envia imagem da arte com descrição do post
 *   2. Envia enquete com opções "✅ Aprovar" e "❌ Rejeitar"
 *   3. Salva o stanza ID da enquete → usado no webhook para matching preciso
 *
 * O cliente vota tocando em uma opção — sem risco de falsos positivos
 * por conversas normais no grupo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    public static final String OPTION_APPROVE = "✅ Aprovar";
    public static final String OPTION_REJECT  = "❌ Rejeitar";

    private final EvolutionApiClient evolutionApiClient;
    private final ApproveRepository  approveRepository;

    /**
     * Envia a arte + enquete de aprovação para o grupo do cliente.
     * O stanza ID da enquete é salvo no ApproveEntity para matching no webhook.
     */
    @Async
    public void sendApprovalRequest(ClientEntity client, PostEntity post,
                                    ApproveEntity approve, String mediaUrl) {
        String groupId = client.getWhatsappGroupId();
        if (!StringUtils.hasText(groupId)) {
            log.warn("[WhatsApp] Cliente '{}' sem grupo vinculado. Aprovação não enviada.", client.getName());
            return;
        }

        try {
            // Passo 1: envia a imagem da arte com descrição
            String imageCaption = buildImageCaption(post, approve);
            evolutionApiClient.sendMediaToGroup(groupId, mediaUrl, imageCaption);
            log.info("[WhatsApp] Arte enviada | grupo: '{}' | Post ID: {}",
                    client.getWhatsappGroupName(), post.getId());

            // Passo 2: envia a enquete logo em seguida
            String pollQuestion = buildPollQuestion(post);
            String pollStanzaId = evolutionApiClient.sendPollToGroup(
                    groupId,
                    pollQuestion,
                    List.of(OPTION_APPROVE, OPTION_REJECT),
                    1 // seleção única
            );

            // Salva o stanza ID da enquete para identificar o voto no webhook
            savePollStanzaId(approve, pollStanzaId);
            log.info("[WhatsApp] Enquete enviada | Post ID: {} | Stanza: {}",
                    post.getId(), pollStanzaId);

        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao enviar aprovação para cliente '{}': {}",
                    client.getName(), e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de rejeição de post para o grupo do cliente.
     */
    @Async
    public void sendRejectionNotification(ClientEntity client, PostEntity post, String rejectionReason) {
        String groupId = client.getWhatsappGroupId();
        if (!StringUtils.hasText(groupId)) {
            log.warn("[WhatsApp] Cliente '{}' sem grupo vinculado. Rejeição não notificada.", client.getName());
            return;
        }

        try {
            evolutionApiClient.sendTextToGroup(groupId, buildRejectionMessage(post, rejectionReason));
            log.info("[WhatsApp] Rejeição notificada | grupo: '{}' | Post ID: {}",
                    client.getWhatsappGroupName(), post.getId());
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao notificar rejeição para cliente '{}': {}",
                    client.getName(), e.getMessage(), e);
        }
    }

    private void savePollStanzaId(ApproveEntity approve, String stanzaId) {
        if (!StringUtils.hasText(stanzaId) || approve.getId() == null) return;
        approve.setWhatsappStanzaId(stanzaId);
        approve.setWhatsappSentAt(LocalDateTime.now().toString());
        approveRepository.save(approve);
    }

    private String buildImageCaption(PostEntity post, ApproveEntity approve) {
        StringBuilder sb = new StringBuilder();
        sb.append("📸 *Nova arte para aprovação*\n\n");
        sb.append("*Post:* ").append(post.getTitle()).append("\n");
        sb.append("*Temática:* ").append(post.getTheme()).append("\n");
        sb.append("*Agendado para:* ").append(formatDate(post.getScheduledAt())).append("\n");

        if (StringUtils.hasText(approve.getCaption())) {
            sb.append("\n*Legenda sugerida:*\n").append(approve.getCaption());
        }

        return sb.toString();
    }

    private String buildPollQuestion(PostEntity post) {
        return "📊 Aprovar arte do post: " + post.getTitle() + "?";
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
