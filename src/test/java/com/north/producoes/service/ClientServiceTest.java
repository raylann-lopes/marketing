package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ClientRequestDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.*;

@DisplayName("ClientService")
@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private FinanceService financeService;

    @InjectMocks
    private ClientService clientService;

    @Nested
    @DisplayName("findAllClient()")
    class FindAllClient {

        @Test
        @DisplayName("deve retornar lista de clientes")
        void shouldReturnAllClients() {
            ClientEntity client = new ClientEntity();
            client.setName("John Doe");
            when(clientRepository.findAll()).thenReturn(List.of(client));

            List<ClientEntity> result = clientService.findAllClient();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("deve retornar cliente quando e-mail existe")
        void shouldReturnClientWhenEmailExists() {
            ClientEntity client = new ClientEntity();
            client.setEmail("john@example.com");
            when(clientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(client));

            ClientEntity result = clientService.findByEmail("john@example.com");

            assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando e-mail não existe")
        void shouldThrowWhenEmailNotFound() {
            when(clientRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.findByEmail("notfound@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("notfound@example.com");
        }
    }

    @Nested
    @DisplayName("findByNumber()")
    class FindByNumber {

        @Test
        @DisplayName("deve retornar cliente quando número existe")
        void shouldReturnClientWhenNumberExists() {
            ClientEntity client = new ClientEntity();
            client.setNumber("11999999999");
            when(clientRepository.findByNumber("11999999999")).thenReturn(Optional.of(client));

            ClientEntity result = clientService.findByNumber("11999999999");

            assertThat(result.getNumber()).isEqualTo("11999999999");
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando número não existe")
        void shouldThrowWhenNumberNotFound() {
            when(clientRepository.findByNumber("00000000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.findByNumber("00000000000"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("00000000000");
        }
    }

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("deve retornar clientes com o status informado")
        void shouldReturnClientsWithGivenStatus() {
            ClientEntity client = new ClientEntity();
            client.setStatus(ClientStatusEnum.ACTIVE);
            when(clientRepository.findByStatus(ClientStatusEnum.ACTIVE)).thenReturn(List.of(client));

            List<ClientEntity> result = clientService.findByStatus(ClientStatusEnum.ACTIVE);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando nenhum cliente tem o status")
        void shouldThrowWhenNoClientWithStatus() {
            when(clientRepository.findByStatus(ClientStatusEnum.INACTIVE)).thenReturn(List.of());

            assertThatThrownBy(() -> clientService.findByStatus(ClientStatusEnum.INACTIVE))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("saveClient()")
    class SaveClient {

        @Test
        @DisplayName("deve salvar e retornar o cliente quando e-mail não existe")
        void shouldSaveClientWhenEmailIsNew() {
            ClientRequestDTO request = new ClientRequestDTO(
                    "New Client",
                    "new@example.com",
                    "11999999999",
                    "https://drive.google.com/abc",
                    "Formal",
                    "Saude",
                    500.0
            );

            when(clientRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(clientRepository.findByNumber("11999999999")).thenReturn(Optional.empty());
            when(clientRepository.save(any(ClientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ClientEntity result = clientService.saveClient(request);

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            assertThat(result.getNumber()).isEqualTo("11999999999");
            verify(clientRepository).save(any(ClientEntity.class));
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando e-mail já cadastrado")
        void shouldThrowWhenEmailAlreadyExists() {
            ClientRequestDTO request = new ClientRequestDTO(
                    "Existing Client",
                    "existing@example.com",
                    "11999999999",
                    "https://drive.google.com/abc",
                    "Formal",
                    "Saude",
                    500.0
            );

            when(clientRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new ClientEntity()));

            assertThatThrownBy(() -> clientService.saveClient(request))
                    .isInstanceOf(ResourceAlreadyExistsException.class);

            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateClient()")
    class UpdateClient {

        @Test
        @DisplayName("deve atualizar e retornar o cliente quando existe")
        void shouldUpdateClientWhenExists() {
            ClientEntity existingClient = new ClientEntity();
            existingClient.setId(1L);
            existingClient.setStatus(ClientStatusEnum.INACTIVE);

            ClientRequestDTO request = new ClientRequestDTO(
                    "Client Name",
                    "client@example.com",
                    "11999999999",
                    "https://drive.google.com/abc",
                    "Formal",
                    "Saude",
                    500.0
            );

            when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
            when(clientRepository.save(existingClient)).thenReturn(existingClient);

            ClientEntity result = clientService.updateClient(1L, request);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Client Name");
            assertThat(result.getEmail()).isEqualTo("client@example.com");
            assertThat(result.getStatus()).isEqualTo(ClientStatusEnum.INACTIVE);
            verify(clientRepository).save(existingClient);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não existe")
        void shouldThrowWhenClientNotFound() {
            ClientRequestDTO request = new ClientRequestDTO(
                    "Client Name",
                    "client@example.com",
                    "11999999999",
                    "https://drive.google.com/abc",
                    "Formal",
                    "Saude",
                    500.0
            );

            when(clientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClient(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando e-mail já pertence a outro cliente")
        void shouldThrowWhenEmailAlreadyUsedByAnotherClient() {
            ClientEntity existingClient = new ClientEntity();
            existingClient.setId(1L);

            ClientRequestDTO request = new ClientRequestDTO(
                    "Client Name",
                    "duplicated@example.com",
                    "11999999999",
                    "https://drive.google.com/abc",
                    "Formal",
                    "Saude",
                    500.0
            );

            when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
            when(clientRepository.existsByEmailAndIdNot("duplicated@example.com", 1L)).thenReturn(true);

            assertThatThrownBy(() -> clientService.updateClient(1L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("duplicated@example.com");

            verify(clientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando número já pertence a outro cliente")
        void shouldThrowWhenNumberAlreadyUsedByAnotherClient() {
            ClientEntity existingClient = new ClientEntity();
            existingClient.setId(1L);

            ClientRequestDTO request = new ClientRequestDTO(
                    "Client Name",
                    "client@example.com",
                    "11888888888",
                    "https://drive.google.com/abc",
                    "Formal",
                    "Saude",
                    500.0
            );

            when(clientRepository.findById(1L)).thenReturn(Optional.of(existingClient));
            when(clientRepository.existsByNumberAndIdNot("11888888888", 1L)).thenReturn(true);

            assertThatThrownBy(() -> clientService.updateClient(1L, request))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("11888888888");

            verify(clientRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteClientById()")
    class DeleteClientById {

        @Test
        @DisplayName("deve deletar cliente quando existe")
        void shouldDeleteClientWhenExists() {
            when(clientRepository.existsById(1L)).thenReturn(true);

            clientService.deleteClientById(1L);

            verify(clientRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não existe")
        void shouldThrowWhenClientNotFound() {
            when(clientRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> clientService.deleteClientById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            verify(clientRepository, never()).deleteById(anyLong());
        }
    }
}
