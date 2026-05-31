package com.north.producoes.integration.evolutionApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta da Evolution API ao enviar uma mensagem.
 * O campo key.id contém o stanza ID da mensagem enviada.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionSentMessageDTO(Key key) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Key(String id, Boolean fromMe, String remoteJid) {}
}
