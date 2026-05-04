package com.north.producoes.service;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.AccountConfigRepository;
import com.north.producoes.repository.ClientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccountConfigService")
@ExtendWith(MockitoExtension.class)
class AccountConfigServiceTest {

    @Mock
    private AccountConfigRepository accountConfigRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private AccountConfigService accountConfigService;

    @Nested
    @DisplayName("configure()")
    class Configure {

        @Test
        @DisplayName("deve criar configuração quando cliente e Instagram ainda não estão configurados")
        void shouldCreateConfigWhenClientAndInstagramAreAvailable() {
            // Arrange
            AccountConfigRequestDTO request = new AccountConfigRequestDTO(1L, "1784140000", " token-value ");
            ClientEntity client = client(1L);
            when(accountConfigRepository.existsByClientId(1L)).thenReturn(false);
            when(accountConfigRepository.findByIgUserId("1784140000")).thenReturn(Optional.empty());
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(accountConfigRepository.save(any(AccountConfigEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            AccountConfigEntity result = accountConfigService.configure(request, "admin@example.com");

            // Assert
            assertThat(result.getClient()).isSameAs(client);
            assertThat(result.getIgUserId()).isEqualTo("1784140000");
            assertThat(result.getAccessToken()).isEqualTo("token-value");
            assertThat(result.getConfiguredBy()).isEqualTo("admin@example.com");
            assertThat(result.getConfiguredAt()).isNotNull();

            ArgumentCaptor<AccountConfigEntity> captor = ArgumentCaptor.forClass(AccountConfigEntity.class);
            verify(accountConfigRepository).save(captor.capture());
            assertThat(captor.getValue().getAccessToken()).isEqualTo("token-value");
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando cliente já possui configuração")
        void shouldThrowWhenClientAlreadyHasConfig() {
            // Arrange
            AccountConfigRequestDTO request = new AccountConfigRequestDTO(1L, "1784140000", "token");
            when(accountConfigRepository.existsByClientId(1L)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> accountConfigService.configure(request, "admin@example.com"))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("cliente ID 1");
            verify(accountConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ResourceAlreadyExistsException quando Instagram já estiver configurado para outro cliente")
        void shouldThrowWhenInstagramAlreadyConfigured() {
            // Arrange
            AccountConfigRequestDTO request = new AccountConfigRequestDTO(1L, "1784140000", "token");
            AccountConfigEntity existing = new AccountConfigEntity();
            existing.setClient(client(2L));
            when(accountConfigRepository.existsByClientId(1L)).thenReturn(false);
            when(accountConfigRepository.findByIgUserId("1784140000")).thenReturn(Optional.of(existing));

            // Act & Assert
            assertThatThrownBy(() -> accountConfigService.configure(request, "admin@example.com"))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessageContaining("cliente ID 2");
            verify(accountConfigRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não existir")
        void shouldThrowWhenClientDoesNotExist() {
            // Arrange
            AccountConfigRequestDTO request = new AccountConfigRequestDTO(99L, "1784140000", "token");
            when(accountConfigRepository.existsByClientId(99L)).thenReturn(false);
            when(accountConfigRepository.findByIgUserId("1784140000")).thenReturn(Optional.empty());
            when(clientRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountConfigService.configure(request, "admin@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
            verify(accountConfigRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findByClientId()")
    class FindByClientId {

        @Test
        @DisplayName("deve retornar configuração por cliente")
        void shouldReturnConfigByClientId() {
            // Arrange
            AccountConfigEntity config = config(client(1L), "1784140000", "token");
            when(accountConfigRepository.findByClientId(1L)).thenReturn(Optional.of(config));

            // Act
            AccountConfigEntity result = accountConfigService.findByClientId(1L);

            // Assert
            assertThat(result).isSameAs(config);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando configuração não existir")
        void shouldThrowWhenConfigDoesNotExist() {
            // Arrange
            when(accountConfigRepository.findByClientId(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> accountConfigService.findByClientId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Test
    @DisplayName("deve expor igUserId e accessToken a partir da configuração do cliente")
    void shouldExposeCredentialsByClientId() {
        // Arrange
        AccountConfigEntity config = config(client(1L), "1784140000", "token");
        when(accountConfigRepository.findByClientId(1L)).thenReturn(Optional.of(config));

        // Act & Assert
        assertThat(accountConfigService.getIgUserId(1L)).isEqualTo("1784140000");
        assertThat(accountConfigService.getAccessToken(1L)).isEqualTo("token");
    }

    private static AccountConfigEntity config(ClientEntity client, String igUserId, String accessToken) {
        AccountConfigEntity config = new AccountConfigEntity();
        config.setClient(client);
        config.setIgUserId(igUserId);
        config.setAccessToken(accessToken);
        return config;
    }

    private static ClientEntity client(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName("Cliente " + id);
        return client;
    }
}