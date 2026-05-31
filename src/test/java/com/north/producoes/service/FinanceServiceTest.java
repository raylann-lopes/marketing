package com.north.producoes.service;

import com.north.producoes.controller.dto.request.FinanceRequestDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.FinanceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FinanceService")
@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private FinanceRepository financeRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private FinanceService financeService;

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("deve retornar todos os registros financeiros")
        void shouldReturnAllFinances() {
            FinanceEntity finance = finance(10L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.findAll()).thenReturn(List.of(finance));

            List<FinanceEntity> result = financeService.findAll();

            assertThat(result)
                    .hasSize(1)
                    .extracting(FinanceEntity::getId)
                    .containsExactly(10L);
            verify(financeRepository).findAll();
        }
    }

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("deve retornar registros financeiros quando status existir")
        void shouldReturnFinancesWhenStatusExists() {
            FinanceEntity finance = finance(10L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.findByStatus(FinanceStatusEnum.PENDING)).thenReturn(List.of(finance));

            List<FinanceEntity> result = financeService.findByStatus(FinanceStatusEnum.PENDING);

            assertThat(result)
                    .hasSize(1)
                    .extracting(FinanceEntity::getStatus)
                    .containsExactly(FinanceStatusEnum.PENDING);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não existirem registros com o status")
        void shouldReturnEmptyListWhenNoFinancesWithStatus() {
            when(financeRepository.findByStatus(FinanceStatusEnum.PAY)).thenReturn(List.of());

            List<FinanceEntity> result = financeService.findByStatus(FinanceStatusEnum.PAY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByClientId()")
    class FindByClientId {

        @Test
        @DisplayName("deve retornar registros financeiros do cliente")
        void shouldReturnFinancesWhenClientExists() {
            ClientEntity client = client(1L);
            FinanceEntity finance = finance(10L, client, FinanceStatusEnum.PENDING);
            when(financeRepository.findByClientId(1L)).thenReturn(List.of(finance));

            List<FinanceEntity> result = financeService.findByClientId(1L);

            assertThat(result)
                    .hasSize(1)
                    .extracting(FinanceEntity::getClient)
                    .containsExactly(client);
            verify(financeRepository).findByClientId(1L);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando cliente não tiver registros")
        void shouldReturnEmptyListWhenClientHasNoFinances() {
            when(financeRepository.findByClientId(99L)).thenReturn(List.of());

            List<FinanceEntity> result = financeService.findByClientId(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveFinance()")
    class SaveFinance {

        @Test
        @DisplayName("deve salvar registro financeiro simples")
        void shouldSaveFinance() {
            ClientEntity client = client(1L);
            FinanceRequestDTO dto = new FinanceRequestDTO(
                    1L, "Mensalidade", BigDecimal.valueOf(1500),
                    FinanceStatusEnum.PENDING, null,
                    LocalDate.of(2026, 5, 10), null
            );
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(financeRepository.save(any(FinanceEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            FinanceEntity result = financeService.saveFinance(dto, null);

            assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(result.getClient()).isEqualTo(client);
            verify(financeRepository).save(any(FinanceEntity.class));
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não existir")
        void shouldThrowWhenClientNotFound() {
            FinanceRequestDTO dto = new FinanceRequestDTO(
                    99L, "Mensalidade", BigDecimal.valueOf(1500),
                    null, null, LocalDate.of(2026, 5, 10), null
            );
            when(clientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> financeService.saveFinance(dto, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
            verify(financeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateFinance()")
    class UpdateFinance {

        @Test
        @DisplayName("deve atualizar registro quando existir")
        void shouldUpdateFinanceWhenExists() {
            ClientEntity client = client(1L);
            FinanceEntity existing = finance(10L, client, FinanceStatusEnum.PENDING);
            FinanceRequestDTO dto = new FinanceRequestDTO(
                    1L, "Novo descritivo", BigDecimal.valueOf(2000),
                    FinanceStatusEnum.PENDING, null,
                    LocalDate.of(2026, 6, 10), null
            );
            when(financeRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(financeRepository.save(existing)).thenReturn(existing);

            FinanceEntity result = financeService.updateFinance(10L, dto);

            assertThat(result.getValue()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            assertThat(result.getDescription()).isEqualTo("Novo descritivo");
            verify(financeRepository).findById(10L);
            verify(financeRepository).save(existing);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando registro não existir")
        void shouldThrowWhenFinanceDoesNotExist() {
            FinanceRequestDTO dto = new FinanceRequestDTO(
                    1L, "Descritivo", BigDecimal.valueOf(1500),
                    null, null, LocalDate.of(2026, 5, 10), null
            );
            when(financeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> financeService.updateFinance(99L, dto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
            verify(financeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteFinanceById()")
    class DeleteFinanceById {

        @Test
        @DisplayName("deve deletar registro financeiro quando existir")
        void shouldDeleteFinanceWhenExists() {
            when(financeRepository.existsById(10L)).thenReturn(true);

            financeService.deleteFinanceById(10L);

            verify(financeRepository).deleteById(10L);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando registro não existir")
        void shouldThrowWhenFinanceDoesNotExist() {
            when(financeRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> financeService.deleteFinanceById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
            verify(financeRepository, never()).deleteById(99L);
        }
    }

    private static ClientEntity client(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName("John Doe");
        return client;
    }

    private static FinanceEntity finance(Long id, ClientEntity client, FinanceStatusEnum status) {
        FinanceEntity finance = new FinanceEntity();
        finance.setId(id);
        finance.setClient(client);
        finance.setDescription("Mensalidade");
        finance.setValue(BigDecimal.valueOf(1500));
        finance.setStatus(status);
        finance.setExpirationDate(LocalDateTime.of(2026, 5, 10, 0, 0));
        finance.setPaymentDate(LocalDateTime.of(2026, 5, 10, 0, 0));
        return finance;
    }
}
