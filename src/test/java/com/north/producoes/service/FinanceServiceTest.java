package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.enums.FinanceStatusEnum;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            // Arrange
            FinanceEntity finance = finance(10L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.findAll()).thenReturn(List.of(finance));

            // Act
            List<FinanceEntity> result = financeService.findAll();

            // Assert
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
            // Arrange
            FinanceEntity finance = finance(10L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.findByStatus(FinanceStatusEnum.PENDING)).thenReturn(List.of(finance));

            // Act
            List<FinanceEntity> result = financeService.findByStatus(FinanceStatusEnum.PENDING);

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .extracting(FinanceEntity::getStatus)
                    .containsExactly(FinanceStatusEnum.PENDING);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando status não tiver registros")
        void shouldThrowWhenStatusHasNoFinances() {
            // Arrange
            when(financeRepository.findByStatus(FinanceStatusEnum.PAY)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> financeService.findByStatus(FinanceStatusEnum.PAY))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Conta com status");
        }
    }

    @Nested
    @DisplayName("findByClientId()")
    class FindByClientId {

        @Test
        @DisplayName("deve retornar registros financeiros do cliente")
        void shouldReturnFinancesWhenClientExists() {
            // Arrange
            ClientEntity client = client(1L);
            FinanceEntity finance = finance(10L, client, FinanceStatusEnum.PENDING);
            when(financeRepository.findByClientId(1L)).thenReturn(List.of(finance));

            // Act
            List<FinanceEntity> result = financeService.findByClientId(1L);

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .extracting(FinanceEntity::getClient)
                    .containsExactly(client);
            verify(financeRepository).findByClientId(1L);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não tiver registros")
        void shouldThrowWhenClientHasNoFinances() {
            // Arrange
            when(financeRepository.findByClientId(99L)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> financeService.findByClientId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("saveFinance()")
    class SaveFinance {

        @Test
        @DisplayName("deve salvar registro financeiro")
        void shouldSaveFinance() {
            // Arrange
            FinanceEntity finance = finance(10L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.save(finance)).thenReturn(finance);

            // Act
            FinanceEntity result = financeService.saveFinance(finance);

            // Assert
            assertThat(result).isSameAs(finance);
            verify(financeRepository).save(finance);
        }
    }

    @Nested
    @DisplayName("updateFinance()")
    class UpdateFinance {

        @Test
        @DisplayName("deve atualizar o id e salvar quando registro existir")
        void shouldUpdateFinanceWhenExists() {
            // Arrange
            FinanceEntity finance = finance(5L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.existsById(5L)).thenReturn(true);
            when(financeRepository.save(finance)).thenReturn(finance);

            // Act
            FinanceEntity result = financeService.updateFinance(10L, finance);

            // Assert
            assertThat(result.getId()).isEqualTo(10L);
            verify(financeRepository).existsById(5L);
            verify(financeRepository).save(finance);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando registro não existir")
        void shouldThrowWhenFinanceDoesNotExist() {
            // Arrange
            FinanceEntity finance = finance(99L, client(1L), FinanceStatusEnum.PENDING);
            when(financeRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> financeService.updateFinance(10L, finance))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
            verify(financeRepository, never()).save(finance);
        }
    }

    @Nested
    @DisplayName("deleteFinanceById()")
    class DeleteFinanceById {

        @Test
        @DisplayName("deve deletar registro financeiro quando existir")
        void shouldDeleteFinanceWhenExists() {
            // Arrange
            when(financeRepository.existsById(10L)).thenReturn(true);

            // Act
            financeService.deleteFinanceById(10L);

            // Assert
            verify(financeRepository).deleteById(10L);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando registro não existir")
        void shouldThrowWhenFinanceDoesNotExist() {
            // Arrange
            when(financeRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
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
        finance.setValue(1500.0);
        finance.setStatus(status);
        finance.setExpirationDate(LocalDateTime.of(2026, 5, 10, 12, 0));
        finance.setPaymentDate(LocalDateTime.of(2026, 5, 11, 12, 0));
        return finance;
    }
}
