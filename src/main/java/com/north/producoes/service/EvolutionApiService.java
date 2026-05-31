package com.north.producoes.service;

import com.north.producoes.controller.dto.request.EvolutionGroupLinkRequestDTO;
import com.north.producoes.controller.dto.response.EvolutionGroupResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;
import com.north.producoes.repository.ClientRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@AllArgsConstructor
public class EvolutionApiService {

    private final EvolutionApiClient evolutionApiClient;
    private final ClientRepository clientRepository;

    public List<EvolutionGroupResponseDTO> findGroups() {
        return evolutionApiClient.findGroups()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EvolutionGroupResponseDTO linkGroupToClient(EvolutionGroupLinkRequestDTO request) {
        EvolutionGroupApiResponseDTO selectedGroup = findGroupById(request.groupId());
        ClientEntity client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado."));

        clientRepository.findByWhatsappGroupId(request.groupId())
                .filter(linkedClient -> !linkedClient.getId().equals(request.clientId()))
                .ifPresent(linkedClient -> {
                    throw new ResourceAlreadyExistsException(
                            "Grupo do WhatsApp ja esta vinculado ao cliente " + linkedClient.getName() + ".");
                });

        client.setWhatsappGroupId(selectedGroup.id());
        client.setWhatsappGroupName(selectedGroup.subject());

        return EvolutionGroupResponseDTO.from(selectedGroup, clientRepository.save(client));
    }

    private EvolutionGroupResponseDTO toResponse(EvolutionGroupApiResponseDTO group) {
        if (!StringUtils.hasText(group.id())) {
            return EvolutionGroupResponseDTO.from(group);
        }

        return clientRepository.findByWhatsappGroupId(group.id())
                .map(client -> EvolutionGroupResponseDTO.from(group, client))
                .orElseGet(() -> EvolutionGroupResponseDTO.from(group));
    }

    private EvolutionGroupApiResponseDTO findGroupById(String groupId) {
        return evolutionApiClient.findGroups()
                .stream()
                .filter(group -> groupId.equals(group.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo do WhatsApp nao encontrado na Evolution API."));
    }
}
