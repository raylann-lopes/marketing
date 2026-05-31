package com.north.producoes.integration.evolutionApi.dto;

import java.util.List;

/**
 * Request para enviar uma enquete (poll) para um grupo WhatsApp via Evolution API.
 * POST /message/sendPoll/{instance}
 */
public record EvolutionSendPollDTO(
        String number,
        String name,
        int selectableCount,
        List<String> values
) {}
