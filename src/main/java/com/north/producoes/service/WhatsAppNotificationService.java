package com.north.producoes.service;

import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Envia notificações WhatsApp diretamente via Evolution API.
 *
 * Todos os métodos públicos são @Async — não bloqueiam o fluxo principal.
 *
 * Importante: os métodos recebem IDs (não entidades JPA) e re-buscam as
 * entidades dentro da própria transação @Async. Isso evita o erro
 * "ResultSet is closed" causado por lazy proxies Hibernate sendo
 * acessados após o fechamento da sessão JPA original.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    public static final String OPTION_APPROVE = "✅ Aprovar";
    public static final String OPTION_REJECT  = "❌ Rejeitar";

    private final EvolutionApiClient evolutionApiClient;
    private final ApproveRepository  approveRepository;
    private final ClientRepository   clientRepository;
    private final PostRepository     postRepository;

    /**
     * Envia imagem da arte + enquete de aprovação para o grupo do cliente.
     *
     * @param clientId  ID do cliente (para buscar groupId com sessão fresca)
     * @param postId    ID do post
     * @param approveId ID da aprovação (para salvar o stanza ID da enquete)
     * @param mediaUrl  URL pública da imagem
     */
    @Async
    @Transactional
    public void sendApprovalRequest(Long clientId, Long postId, Long approveId, List<String> mediaUrls) {
        ClientEntity client  = clientRepository.findById(clientId).orElse(null);
        PostEntity   post    = postRepository.findById(postId).orElse(null);
        ApproveEntity approve = approveRepository.findById(approveId).orElse(null);

        if (client == null || post == null || approve == null) {
            log.warn("[WhatsApp] Entidade não encontrada ao enviar aprovação " +
                     "| clientId={} postId={} approveId={}", clientId, postId, approveId);
            return;
        }

        String groupId = client.getWhatsappGroupId();
        if (!StringUtils.hasText(groupId)) {
            log.warn("[WhatsApp] Cliente '{}' sem grupo WhatsApp vinculado.", client.getName());
            return;
        }

        try {
            // Passo 1: envia a(s) imagem(ns) sem legenda para o WhatsApp tentar agrupar em álbum
            for (String mediaUrl : mediaUrls) {
                evolutionApiClient.sendMediaToGroup(groupId, mediaUrl, null);
            }
            log.info("[WhatsApp] Arte(s) enviada(s) sem legenda | grupo: '{}' | Post ID: {}",
                    client.getWhatsappGroupName(), post.getId());

            // Passo 2: envia a legenda completa como uma mensagem de texto separada
            evolutionApiClient.sendTextToGroup(groupId, buildImageCaption(post, approve));
            log.info("[WhatsApp] Legenda enviada | grupo: '{}' | Post ID: {}",
                    client.getWhatsappGroupName(), post.getId());

            // Passo 3: envia enquete logo em seguida
            String pollStanzaId = evolutionApiClient.sendPollToGroup(
                    groupId,
                    buildPollQuestion(post),
                    List.of(OPTION_APPROVE, OPTION_REJECT),
                    1
            );

            // Salva o stanza ID da enquete para identificar o voto no webhook
            if (StringUtils.hasText(pollStanzaId)) {
                approve.setWhatsappStanzaId(pollStanzaId);
                approve.setWhatsappSentAt(LocalDateTime.now().toString());
                approveRepository.save(approve);
            }

            log.info("[WhatsApp] Enquete enviada | Post ID: {} | Stanza: {}", post.getId(), pollStanzaId);

        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao enviar aprovação para cliente '{}': {}",
                    client.getName(), e.getMessage(), e);
        }
    }

    /**
     * Envia notificação de rejeição para o grupo do cliente.
     *
     * @param clientId ID do cliente
     * @param postId   ID do post rejeitado
     * @param reason   Motivo da rejeição (pode ser null)
     */
    @Async
    @Transactional
    public void sendRejectionNotification(Long clientId, Long postId, String reason) {
        ClientEntity client = clientRepository.findById(clientId).orElse(null);
        PostEntity   post   = postRepository.findById(postId).orElse(null);

        if (client == null || post == null) {
            log.warn("[WhatsApp] Entidade não encontrada ao notificar rejeição " +
                     "| clientId={} postId={}", clientId, postId);
            return;
        }

        String groupId = client.getWhatsappGroupId();
        if (!StringUtils.hasText(groupId)) {
            log.warn("[WhatsApp] Cliente '{}' sem grupo vinculado. Rejeição não notificada.", client.getName());
            return;
        }

        try {
            evolutionApiClient.sendTextToGroup(groupId, buildRejectionMessage(post, reason));
            log.info("[WhatsApp] Rejeição notificada | grupo: '{}' | Post ID: {}",
                    client.getWhatsappGroupName(), post.getId());
        } catch (Exception e) {
            log.error("[WhatsApp] Falha ao notificar rejeição para cliente '{}': {}",
                    client.getName(), e.getMessage(), e);
        }
    }

    // ─── Mensagens ────────────────────────────────────────────────────────────

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
