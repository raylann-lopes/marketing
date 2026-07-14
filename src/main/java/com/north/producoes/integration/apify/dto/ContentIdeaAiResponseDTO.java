package com.north.producoes.integration.apify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Contrato JSON da resposta da IA ao transformar sinais virais em ideias.
 * format: REELS | CAROUSEL | STORIES | FEED
 * priority: HIGH | MEDIUM | LOW
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContentIdeaAiResponseDTO(List<GeneratedIdea> ideas) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeneratedIdea(
            String title,
            String hook,
            String theme,
            String objective,
            String format,
            String reason,
            String priority,
            String signalSummary,
            // Índice 1-based do sinal (na lista enviada no prompt) que
            // inspirou a ideia — vincula cada ideia ao engajamento real
            Integer signalIndex
    ) {
    }
}
