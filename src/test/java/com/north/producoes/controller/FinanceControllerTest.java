package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.FinanceRequestDTO;
import com.north.producoes.controller.dto.response.FinanceResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import com.north.producoes.service.FinanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        when(financeService.findFiltered(null, null, null)).thenReturn(List.of(finance(10L)));

        ResponseEntity<List<FinanceResponseDTO>> response = financeController.findAll(null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .hasSize(1)
                .extracting(FinanceResponseDTO::id)
                .containsExactly(10L);
    }

    @Test
    @DisplayName("deve criar registro financeiro com status 201")
    void shouldCreateFinanceWithCreatedStatus() {
        FinanceRequestDTO request = new FinanceRequestDTO(
                1L, "Mensalidade", BigDecimal.valueOf(1500),
                FinanceStatusEnum.PENDING, FinanceTypeEnum.FIXED_REVENUE,
                LocalDate.of(2026, 5, 10), null
        );
        FinanceEntity saved = finance(10L);
        when(financeService.saveFinance(request, null)).thenReturn(saved);

        ResponseEntity<FinanceResponseDTO> response = financeController.saveFinance(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("deve deletar registro financeiro com status 204")
    void shouldDeleteFinanceWithNoContentStatus() {
        ResponseEntity<Void> response = financeController.deleteFinanceById(10L);

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
        finance.setValue(BigDecimal.valueOf(1500));
        finance.setStatus(FinanceStatusEnum.PENDING);
        finance.setExpirationDate(LocalDateTime.of(2026, 5, 10, 0, 0));
        finance.setPaymentDate(LocalDateTime.of(2026, 5, 10, 0, 0));
        return finance;
    }
}
