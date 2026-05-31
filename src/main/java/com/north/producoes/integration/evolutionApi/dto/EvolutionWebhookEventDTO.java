package com.north.producoes.integration.evolutionApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Payload recebido da Evolution API quando uma mensagem chega em um grupo.
 * event = "messages.upsert" para mensagens novas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionWebhookEventDTO(
        String event,
        String instance,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(Key key, Message message, String messageType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Key(String remoteJid, Boolean fromMe, String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String conversation, ExtendedText extendedTextMessage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExtendedText(String text, ContextInfo contextInfo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContextInfo(String stanzaId) {}
}
