package com.north.producoes.controller;

import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.service.FinanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FinanceController")
@ExtendWith(MockitoExtension.class)
class FinanceControllerTest {

    @Mock
    private FinanceService financeService;

    @InjectMocks
    private FinanceController financeController;

    @Test
    @DisplayName("deve retornar todos os registros financeiros")
    void shouldReturnAllFinances() {
        // Arrange
        when(financeService.findAll()).thenReturn(List.of(finance(10L)));

        // Act
        ResponseEntity<List<FinanceResponseDTO>> response = financeController.findAll();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .hasSize(1)
                .extracting(FinanceResponseDTO::id)
                .containsExactly(10L);
    }

    @Test
    @DisplayName("deve retornar bad request quando status for nulo")
    void shouldReturnBadRequestWhenStatusIsNull() {
        // Act
        ResponseEntity<List<FinanceResponseDTO>> response = financeController.findByStatus(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("deve criar registro financeiro")
    void shouldCreateFinance() {
        // Arrange
        FinanceEntity finance = finance(10L);
        when(financeService.saveFinance(finance)).thenReturn(finance);

        // Act
        ResponseEntity<FinanceResponseDTO> response = financeController.saveFinance(finance);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("deve deletar registro financeiro com status 204")
    void shouldDeleteFinanceWithNoContentStatus() {
        // Act
        ResponseEntity<Void> response = financeController.deleteFinanceById(10L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(financeService).deleteFinanceById(10L);
    }

    private static FinanceEntity finance(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(1L);

        FinanceEntity finance = new FinanceEntity();
        finance.setId(id);
        finance.setClient(client);
        finance.setDescription("Mensalidade");
        finance.setValue(1500.0);
        finance.setStatus(FinanceStatusEnum.PENDING);
        finance.setExpirationDate(LocalDateTime.of(2026, 5, 10, 12, 0));
        return finance;
    }
}
