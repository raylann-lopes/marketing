package com.north.producoes.service;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.request.MetaInstagramAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.MetaAccountsResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramAccountResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramBusinessAccountResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.MetaGraphIntegrationException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.meta.MetaGraphClient;
import com.north.producoes.integration.meta.dto.MetaPageResponseDTO;
import com.north.producoes.repository.AccountConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MetaGraphService")
@ExtendWith(MockitoExtension.class)
class MetaGraphServiceTest {

    @Mock
    private MetaGraphClient metaGraphClient;

    @Mock
    private AccountConfigRepository accountConfigRepository;

    @Mock
    private AccountConfigService accountConfigService;

    @InjectMocks
    private MetaGraphService metaGraphService;

    @Nested
    @DisplayName("findInstagramAccounts()")
    class FindInstagramAccounts {

        @Test
        @DisplayName("deve retornar apenas páginas com conta Instagram profissional")
        void shouldReturnOnlyPagesWithInstagramBusinessAccount() {
            // Arrange
            MetaPageResponseDTO pageWithInstagram = page("100", "Page A", "page-token", instagram("200", "north"));
            MetaPageResponseDTO pageWithoutInstagram = page("101", "Page B", "page-token", null);
            when(metaGraphClient.findPages()).thenReturn(new MetaAccountsResponseDTO(List.of(pageWithInstagram, pageWithoutInstagram)));
            when(accountConfigRepository.findByIgUserId("200")).thenReturn(Optional.empty());

            // Act
            List<MetaInstagramAccountResponseDTO> result = metaGraphService.findInstagramAccounts();

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .extracting(MetaInstagramAccountResponseDTO::igUserId)
                    .containsExactly("200");
        }

        @Test
        @DisplayName("deve marcar Instagram como vinculado quando já existir configuração")
        void shouldMarkInstagramAccountAsLinked() {
            // Arrange
            MetaPageResponseDTO page = page("100", "Page A", "page-token", instagram("200", "north"));
            AccountConfigEntity config = config(client(1L, "Cliente A"), "200", "token");
            when(metaGraphClient.findPages()).thenReturn(new MetaAccountsResponseDTO(List.of(page)));
            when(accountConfigRepository.findByIgUserId("200")).thenReturn(Optional.of(config));

            // Act
            List<MetaInstagramAccountResponseDTO> result = metaGraphService.findInstagramAccounts();

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(response -> {
                        assertThat(response.alreadyLinked()).isTrue();
                        assertThat(response.linkedClientId()).isEqualTo(1L);
                    });
        }
    }

    @Nested
    @DisplayName("linkInstagramAccount()")
    class LinkInstagramAccount {

        @Test
        @DisplayName("deve configurar conta selecionada usando token da página")
        void shouldConfigureSelectedAccountUsingPageToken() {
            // Arrange
            MetaPageResponseDTO page = page("100", "Page A", "page-token", instagram("200", "north"));
            AccountConfigEntity config = config(client(1L, "Cliente A"), "200", "page-token");
            when(metaGraphClient.findPages()).thenReturn(new MetaAccountsResponseDTO(List.of(page)));
            when(accountConfigService.configure(org.mockito.ArgumentMatchers.any(AccountConfigRequestDTO.class), org.mockito.ArgumentMatchers.eq("admin@example.com")))
                    .thenReturn(config);

            // Act
            AccountConfigEntity result = metaGraphService.linkInstagramAccount(
                    new MetaInstagramAccountLinkRequestDTO(1L, "100", "200"),
                    "admin@example.com"
            );

            // Assert
            assertThat(result).isSameAs(config);
            ArgumentCaptor<AccountConfigRequestDTO> captor = ArgumentCaptor.forClass(AccountConfigRequestDTO.class);
            verify(accountConfigService).configure(captor.capture(), org.mockito.ArgumentMatchers.eq("admin@example.com"));
            assertThat(captor.getValue().clientId()).isEqualTo(1L);
            assertThat(captor.getValue().igUserId()).isEqualTo("200");
            assertThat(captor.getValue().accessToken()).isEqualTo("page-token");
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando página/Instagram não corresponder")
        void shouldThrowWhenSelectedAccountDoesNotMatch() {
            // Arrange
            MetaPageResponseDTO page = page("100", "Page A", "page-token", instagram("200", "north"));
            when(metaGraphClient.findPages()).thenReturn(new MetaAccountsResponseDTO(List.of(page)));

            // Act & Assert
            assertThatThrownBy(() -> metaGraphService.linkInstagramAccount(
                    new MetaInstagramAccountLinkRequestDTO(1L, "100", "999"),
                    "admin@example.com"
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Conta Instagram");
        }

        @Test
        @DisplayName("deve lançar MetaGraphIntegrationException quando Meta não retornar token da página")
        void shouldThrowWhenPageTokenIsMissing() {
            // Arrange
            MetaPageResponseDTO page = page("100", "Page A", " ", instagram("200", "north"));
            when(metaGraphClient.findPages()).thenReturn(new MetaAccountsResponseDTO(List.of(page)));

            // Act & Assert
            assertThatThrownBy(() -> metaGraphService.linkInstagramAccount(
                    new MetaInstagramAccountLinkRequestDTO(1L, "100", "200"),
                    "admin@example.com"
            ))
                    .isInstanceOf(MetaGraphIntegrationException.class)
                    .hasMessageContaining("token");
        }
    }

    private static MetaPageResponseDTO page(
            String id,
            String name,
            String accessToken,
            MetaInstagramBusinessAccountResponseDTO instagram
    ) {
        return new MetaPageResponseDTO(id, name, accessToken, instagram);
    }

    private static MetaInstagramBusinessAccountResponseDTO instagram(String id, String username) {
        return new MetaInstagramBusinessAccountResponseDTO(id, username, "North");
    }

    private static AccountConfigEntity config(ClientEntity client, String igUserId, String accessToken) {
        AccountConfigEntity config = new AccountConfigEntity();
        config.setClient(client);
        config.setIgUserId(igUserId);
        config.setAccessToken(accessToken);
        return config;
    }

    private static ClientEntity client(Long id, String name) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(name);
        return client;
    }
}
