package com.north.producoes.integration.evolutionApi.dto;

public record EvolutionMediaDownloadRequestDTO(
        MessageReference message,
        boolean convertToMp4
) {
    public static EvolutionMediaDownloadRequestDTO from(EvolutionWebhookEventDTO.Key key) {
        return new EvolutionMediaDownloadRequestDTO(new MessageReference(key), false);
    }

    public record MessageReference(EvolutionWebhookEventDTO.Key key) {}
}
