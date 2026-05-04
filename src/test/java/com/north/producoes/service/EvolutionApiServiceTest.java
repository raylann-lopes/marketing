package com.north.producoes.service;

import com.north.producoes.controller.dto.request.EvolutionGroupLinkRequestDTO;
import com.north.producoes.controller.dto.response.EvolutionGroupResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.evolutionApi.EvolutionApiClient;
import com.north.producoes.integration.evolutionApi.dto.EvolutionGroupApiResponseDTO;
import com.north.producoes.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EvolutionApiService")
@ExtendWith(MockitoExtension.class)
class EvolutionApiServiceTest {

    @Mock
    private EvolutionApiClient evolutionApiClient;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private EvolutionApiService evolutionApiService;

    @Nested
    @DisplayName("findGroups()")
    class FindGroups {

        @Test
        @DisplayName("deve marcar grupo como vinculado quando já existir cliente com groupId")
        void shouldMarkGroupAsLinkedWhenClientHasGroupId() {
            // Arrange
            EvolutionGroupApiResponseDTO group = group("group-1", "Clientes VIP");
            ClientEntity client = client(1L, "Cliente A");
            when(evolutionApiClient.findGroups()).thenReturn(List.of(group));
            when(clientRepository.findByWhatsappGroupId("group-1")).thenReturn(Optional.of(client));

            // Act
            List<EvolutionGroupResponseDTO> result = evolutionApiService.findGroups();

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(response -> {
                        assertThat(response.groupId()).isEqualTo("group-1");
                        assertThat(response.alreadyLinked()).isTrue();
                        assertThat(response.linkedClientId()).isEqualTo(1L);
                    });
        }
    }

    @Nested
    @DisplayName("linkGroupToClient()")
    class LinkGroupToClient {

        @Test
        @DisplayName("deve vincular grupo ao cliente")
        void shouldLinkGroupToClient() {
            // Arrange
            EvolutionGroupApiResponseDTO group = group("group-1", "Clientes VIP");
            ClientEntity client = client(1L, "Cliente A");
            when(evolutionApiClient.findGroups()).thenReturn(List.of(group));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(clientRepository.findByWhatsappGroupId("group-1")).thenReturn(Optional.empty());
            when(clientRepository.save(client)).thenReturn(client);

            // Act
            EvolutionGroupResponseDTO result = evolutionApiService.linkGroupToClient(
                    new EvolutionGroupLinkRequestDTO(1L, "group-1")
            );

            // Assert
            assertThat(client.getWhatsappGroupId()).isEqualTo("group-1");
            assertThat(client.getWhatsappGroupName()).isEqualTo("Clientes VIP");
            assertThat(result.alreadyLinked()).isTrue();
            assertThat(result.linkedClientId()).isEqualTo(1L);
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando grupo não existir na Evolution")
        void shouldThrowWhenGroupDoesNotExist() {
            // Arrange
            when(evolutionApiClient.findGroups()).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> evolutionApiService.linkGroupToClient(
                    new EvolutionGroupLinkRequestDTO(1L, "missing")
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Grupo do WhatsApp");
            verify(clientRepository, never()).save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando grupo estiver vinculado a outro cliente")
        void shouldThrowWhenGroupIsLinkedToAnotherClient() {
            // Arrange
            EvolutionGroupApiResponseDTO group = group("group-1", "Clientes VIP");
            ClientEntity client = client(1L, "Cliente A");
            ClientEntity linkedClient = client(2L, "Cliente B");
            when(evolutionApiClient.findGroups()).thenReturn(List.of(group));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(clientRepository.findByWhatsappGroupId("group-1")).thenReturn(Optional.of(linkedClient));

            // Act & Assert
            assertThatThrownBy(() -> evolutionApiService.linkGroupToClient(
                    new EvolutionGroupLinkRequestDTO(1L, "group-1")
            ))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("Cliente B");
            verify(clientRepository, never()).save(client);
        }
    }

    private static EvolutionGroupApiResponseDTO group(String id, String subject) {
        return new EvolutionGroupApiResponseDTO(id, subject, "https://picture.example", 15, "owner", "desc", false, false);
    }

    private static ClientEntity client(Long id, String name) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(name);
        return client;
    }
}
