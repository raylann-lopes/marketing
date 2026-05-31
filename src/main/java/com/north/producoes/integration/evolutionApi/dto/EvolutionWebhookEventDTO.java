package com.north.producoes.integration.evolutionApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Payload recebido da Evolution API quando uma mensagem chega em um grupo.
 *
 * messageType = "conversation"         → mensagem de texto simples
 * messageType = "extendedTextMessage"  → resposta citando outra mensagem
 * messageType = "pollUpdateMessage"    → voto em enquete
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionWebhookEventDTO(
        String event,
        String instance,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Key key,
            Message message,
            String messageType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Key(String remoteJid, Boolean fromMe, String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String conversation,
            ExtendedText extendedTextMessage,
            PollUpdate pollUpdateMessage
    ) {}

    // Resposta citando uma mensagem (reply)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtendedText(String text, ContextInfo contextInfo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextInfo(String stanzaId) {}

    // Voto em enquete
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PollUpdate(
            PollCreationKey pollCreationMessageKey,
            PollVote vote
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PollCreationKey(String remoteJid, Boolean fromMe, String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PollVote(List<PollOption> selectedOptions) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PollOption(String name) {}
}
