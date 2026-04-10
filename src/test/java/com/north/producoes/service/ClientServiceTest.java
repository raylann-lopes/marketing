package com.north.producoes.service;

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
            ClientEntity client = new ClientEntity();
            client.setEmail("new@example.com");
            when(clientRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(clientRepository.save(client)).thenReturn(client);

            ClientEntity result = clientService.saveClient(client);

            assertThat(result.getEmail()).isEqualTo("new@example.com");
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando e-mail já cadastrado")
        void shouldThrowWhenEmailAlreadyExists() {
            ClientEntity client = new ClientEntity();
            client.setEmail("existing@example.com");
            when(clientRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(client));

            assertThatThrownBy(() -> clientService.saveClient(client))
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
            ClientEntity client = new ClientEntity();
            client.setId(1L);
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(clientRepository.save(client)).thenReturn(client);

            ClientEntity result = clientService.updateClient(client);

            assertThat(result.getId()).isEqualTo(1L);
            verify(clientRepository).save(client);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não existe")
        void shouldThrowWhenClientNotFound() {
            ClientEntity client = new ClientEntity();
            client.setId(99L);
            when(clientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clientService.updateClient(client))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

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
