package com.north.producoes.controller.dto.response;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;

public record EvolutionGroupResponseDTO(
        String groupId,
        String groupName,
        String pictureUrl,
        Integer participantsCount,
        boolean alreadyLinked,
        Long linkedClientId,
        String linkedClientName
) {
    public static EvolutionGroupResponseDTO from(EvolutionGroupApiResponseDTO group) {
        return new EvolutionGroupResponseDTO(
                group.id(),
                group.subject(),
                group.pictureUrl(),
                group.size(),
                false,
                null,
                null
        );
    }

    public static EvolutionGroupResponseDTO from(
            EvolutionGroupApiResponseDTO group,
            ClientEntity client
    ) {
        return new EvolutionGroupResponseDTO(
                group.id(),
                group.subject(),
                group.pictureUrl(),
                group.size(),
                true,
                client.getId(),
                client.getName()
        );
    }
}
