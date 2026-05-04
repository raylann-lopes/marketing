package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ClientRequestDTO;
import com.north.producoes.controller.dto.response.ClientResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClientController")
@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    @Nested
    @DisplayName("queries")
    class Queries {

        @Test
        @DisplayName("deve retornar todos os clientes")
        void shouldReturnAllClients() {
            // Arrange
            when(clientService.findAllClient()).thenReturn(List.of(client(1L)));

            // Act
            ResponseEntity<List<ClientResponseDTO>> response = clientController.findAll();

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody())
                    .hasSize(1)
                    .extracting(ClientResponseDTO::id)
                    .containsExactly(1L);
        }

        @Test
        @DisplayName("deve retornar cliente por email")
        void shouldReturnClientByEmail() {
            // Arrange
            when(clientService.findByEmail("client@example.com")).thenReturn(client(1L));

            // Act
            ResponseEntity<ClientResponseDTO> response = clientController.findByEmail("client@example.com");

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().email()).isEqualTo("client@example.com");
        }

        @Test
        @DisplayName("deve retornar clientes por status")
        void shouldReturnClientsByStatus() {
            // Arrange
            when(clientService.findByStatus(ClientStatusEnum.ACTIVE)).thenReturn(List.of(client(1L)));

            // Act
            ResponseEntity<List<ClientResponseDTO>> response = clientController.findByStatus(ClientStatusEnum.ACTIVE);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("commands")
    class Commands {

        @Test
        @DisplayName("deve criar cliente com status 201")
        void shouldCreateClientWithCreatedStatus() {
            // Arrange
            ClientRequestDTO request = request();
            when(clientService.saveClient(request)).thenReturn(client(1L));

            // Act
            ResponseEntity<ClientResponseDTO> response = clientController.saveClient(request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("deve atualizar cliente")
        void shouldUpdateClient() {
            // Arrange
            ClientRequestDTO request = request();
            when(clientService.updateClient(1L, request)).thenReturn(client(1L));

            // Act
            ResponseEntity<ClientResponseDTO> response = clientController.updateClient(1L, request);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(clientService).updateClient(1L, request);
        }

        @Test
        @DisplayName("deve deletar cliente com status 204")
        void shouldDeleteClientWithNoContentStatus() {
            // Act
            ResponseEntity<Void> response = clientController.deleteById(1L);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(clientService).deleteClientById(1L);
        }
    }

    private static ClientRequestDTO request() {
        return new ClientRequestDTO(
                "Cliente",
                "client@example.com",
                "11999999999",
                "https://drive.example",
                "Formal",
                "Saude"
        );
    }

    private static ClientEntity client(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName("Cliente");
        client.setEmail("client@example.com");
        client.setNumber("11999999999");
        client.setDriveLink("https://drive.example");
        client.setVoiceTone("Formal");
        client.setNiche("Saude");
        client.setStatus(ClientStatusEnum.ACTIVE);
        return client;
    }
}
