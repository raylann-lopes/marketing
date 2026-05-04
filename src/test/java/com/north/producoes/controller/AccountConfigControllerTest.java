package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.service.AccountConfigService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccountConfigController")
@ExtendWith(MockitoExtension.class)
class AccountConfigControllerTest {

    @Mock
    private AccountConfigService accountConfigService;

    @InjectMocks
    private AccountConfigController accountConfigController;

    @Test
    @DisplayName("deve configurar conta com status 201 e email do principal")
    void shouldConfigureAccountWithPrincipalEmail() {
        // Arrange
        AccountConfigRequestDTO request = new AccountConfigRequestDTO(1L, "1784140000", "token");
        Principal principal = () -> "admin@example.com";
        when(accountConfigService.configure(request, "admin@example.com")).thenReturn(config());

        // Act
        ResponseEntity<AccountConfigResponseDTO> response = accountConfigController.configure(request, principal);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessTokenMasked()).isEqualTo("********");
        verify(accountConfigService).configure(request, "admin@example.com");
    }

    @Test
    @DisplayName("deve retornar configuração por cliente")
    void shouldReturnConfigByClientId() {
        // Arrange
        when(accountConfigService.findByClientId(1L)).thenReturn(config());

        // Act
        ResponseEntity<AccountConfigResponseDTO> response = accountConfigController.findByClientId(1L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().clientId()).isEqualTo(1L);
    }

    private static AccountConfigEntity config() {
        ClientEntity client = new ClientEntity();
        client.setId(1L);

        AccountConfigEntity config = new AccountConfigEntity();
        config.setId(10L);
        config.setClient(client);
        config.setIgUserId("1784140000");
        config.setAccessToken("token");
        config.setConfiguredBy("admin@example.com");
        config.setConfiguredAt(LocalDateTime.of(2026, 5, 10, 10, 0));
        return config;
    }
}
