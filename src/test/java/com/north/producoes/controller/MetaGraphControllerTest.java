package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.MetaInstagramAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramAccountResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.service.MetaGraphService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("MetaGraphController")
@ExtendWith(MockitoExtension.class)
class MetaGraphControllerTest {

    @Mock
    private MetaGraphService metaGraphService;

    @InjectMocks
    private MetaGraphController metaGraphController;

    @Test
    @DisplayName("deve listar contas Instagram")
    void shouldListInstagramAccounts() {
        // Arrange
        MetaInstagramAccountResponseDTO account = new MetaInstagramAccountResponseDTO(
                "100",
                "Page",
                "200",
                "north",
                "North",
                false,
                null,
                null
        );
        when(metaGraphService.findInstagramAccounts()).thenReturn(List.of(account));

        // Act
        ResponseEntity<List<MetaInstagramAccountResponseDTO>> response = metaGraphController.findInstagramAccounts();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(account);
    }

    @Test
    @DisplayName("deve vincular conta Instagram com status 201")
    void shouldLinkInstagramAccountWithCreatedStatus() {
        // Arrange
        MetaInstagramAccountLinkRequestDTO request = new MetaInstagramAccountLinkRequestDTO(1L, "100", "200");
        Principal principal = () -> "admin@example.com";
        when(metaGraphService.linkInstagramAccount(request, "admin@example.com")).thenReturn(config());

        // Act
        ResponseEntity<AccountConfigResponseDTO> response = metaGraphController.linkInstagramAccount(request, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().clientId()).isEqualTo(1L);
        verify(metaGraphService).linkInstagramAccount(request, "admin@example.com");
    }

    private static AccountConfigEntity config() {
        ClientEntity client = new ClientEntity();
        client.setId(1L);

        AccountConfigEntity config = new AccountConfigEntity();
        config.setId(10L);
        config.setClient(client);
        config.setIgUserId("200");
        config.setAccessToken("token");
        config.setConfiguredBy("admin@example.com");
        config.setConfiguredAt(LocalDateTime.of(2026, 5, 10, 10, 0));
        return config;
    }
}
