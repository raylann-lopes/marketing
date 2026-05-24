package com.north.producoes.service;

import com.north.producoes.controller.dto.response.ForecastResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.FinanceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class FinanceService {
    private final FinanceRepository financeRepository;
    private final ClientRepository clientRepository;

    public List<FinanceEntity> findAll(){
        return financeRepository.findAll();
    }

    public List<FinanceEntity> findByStatus(FinanceStatusEnum status){
        List<FinanceEntity> financeStatus = financeRepository.findByStatus(status);
        if (financeStatus.isEmpty()){
            throw new ResourceNotFoundException("Conta com status" + status + "nao encontrada!");
        }
        return financeStatus;
    }

    public List<FinanceEntity> findByClientId(Long id){
        List<FinanceEntity> financeClient = financeRepository.findByClientId(id);
        if (financeClient.isEmpty()){
            throw new ResourceNotFoundException("Conta do cliente nao encontrada: id " + id);
        }
        return financeClient;
    }

    public FinanceEntity saveFinance(FinanceEntity finance){
        return financeRepository.save(finance);
    }

    public FinanceEntity updateFinance(Long id, FinanceEntity finance){
        if (!financeRepository.existsById(finance.getId())){
            throw new ResourceNotFoundException("Conta nao encontrada: id " + finance.getId());
        }
        finance.setId(id);
        return financeRepository.save(finance);
    }

    public void deleteFinanceById(Long id){
        if (!financeRepository.existsById(id)){
            throw new ResourceNotFoundException("Conta nao encontrada: id " + id);
        }
        financeRepository.deleteById(id);
    }

    // Gera a entrada do mês atual para um cliente (sem duplicar)
    @Transactional
    public void generateCurrentMonthEntry(ClientEntity client, UserEntity createdBy) {
        if (client.getMonthlyValue() == null || client.getMonthlyValue() <= 0) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        boolean exists = !financeRepository.findByClientIdAndExpirationDateBetween(client.getId(), monthStart, monthEnd).isEmpty();
        if (exists) return;

        String monthLabel = now.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));

        FinanceEntity entry = new FinanceEntity();
        entry.setClient(client);
        entry.setUser(createdBy);
        entry.setDescription("Mensalidade " + client.getName() + " - " + monthLabel + " " + now.getYear());
        entry.setValue(client.getMonthlyValue());
        entry.setStatus(FinanceStatusEnum.PENDING);
        entry.setExpirationDate(monthStart.plusDays(4));
        entry.setPaymentDate(monthStart.plusDays(4));
        financeRepository.save(entry);
    }

    // Atualiza o valor de todas as parcelas PENDING do cliente
    @Transactional
    public void updatePendingEntriesValue(Long clientId, Double newValue) {
        List<FinanceEntity> pending = financeRepository.findByClientIdAndStatus(clientId, FinanceStatusEnum.PENDING);
        for (FinanceEntity entry : pending) {
            entry.setValue(newValue);
        }
        financeRepository.saveAll(pending);
    }

    // Remove entradas PENDING a partir do próximo mês (mantém o mês corrente)
    @Transactional
    public void removeUpcomingPendingEntries(Long clientId) {
        LocalDateTime endOfCurrentMonth = LocalDateTime.now()
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                .plusMonths(1).minusSeconds(1);
        financeRepository.deleteByClientIdAndStatusAndExpirationDateAfter(
                clientId, FinanceStatusEnum.PENDING, endOfCurrentMonth);
    }

    public ForecastResponseDTO getForecast(int year) {
        // Inclui ativos e inativos que tenham histórico financeiro no ano
        List<ClientEntity> allClients = clientRepository.findAll().stream()
                .filter(c -> c.getMonthlyValue() != null && c.getMonthlyValue() > 0)
                .toList();

        List<ClientEntity> activeClients = allClients.stream()
                .filter(c -> c.getStatus() == ClientStatusEnum.ACTIVE)
                .toList();

        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<FinanceEntity> yearTransactions = financeRepository.findByExpirationDateBetween(yearStart, yearEnd);

        // Agrupa: clientId -> monthKey -> transações
        Map<Long, Map<String, List<FinanceEntity>>> txByClientMonth = yearTransactions.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getClient().getId(),
                        Collectors.groupingBy(e -> {
                            LocalDateTime d = e.getExpirationDate();
                            return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                        })
                ));

        // Totais consideram apenas clientes ativos
        Double monthlyTotal = activeClients.stream().mapToDouble(ClientEntity::getMonthlyValue).sum();
        Double annualTotal = monthlyTotal * 12;

        // Filtra apenas clientes que participam do ano: ativos + inativos com alguma transação no ano
        List<ClientEntity> visibleClients = allClients.stream()
                .filter(c -> c.getStatus() == ClientStatusEnum.ACTIVE
                        || txByClientMonth.containsKey(c.getId()))
                .toList();

        List<ForecastResponseDTO.ClientForecastDTO> clientDTOs = visibleClients.stream()
                .map(c -> {
                    boolean isActive = c.getStatus() == ClientStatusEnum.ACTIVE;
                    Map<String, ForecastResponseDTO.ClientMonthDTO> monthData = new LinkedHashMap<>();
                    Map<String, List<FinanceEntity>> clientMonths = txByClientMonth.getOrDefault(c.getId(), Map.of());
                    for (int m = 1; m <= 12; m++) {
                        String monthKey = year + "-" + String.format("%02d", m);
                        List<FinanceEntity> entries = clientMonths.getOrDefault(monthKey, List.of());
                        double received = entries.stream().filter(e -> e.getStatus() == FinanceStatusEnum.PAY).mapToDouble(FinanceEntity::getValue).sum();
                        double pending = entries.stream().filter(e -> e.getStatus() == FinanceStatusEnum.PENDING).mapToDouble(FinanceEntity::getValue).sum();
                        // Inativos: sem projeção — só mostra o que existe em transações reais
                        double expected = entries.isEmpty()
                                ? (isActive ? c.getMonthlyValue() : 0.0)
                                : received + pending;
                        if (entries.isEmpty() && !isActive) continue; // omite meses sem dados para inativos
                        monthData.put(monthKey, new ForecastResponseDTO.ClientMonthDTO(expected, received, pending));
                    }
                    return new ForecastResponseDTO.ClientForecastDTO(
                            c.getId(), c.getName(), c.getNiche(), c.getMonthlyValue(),
                            c.getStatus().name(), monthData);
                })
                .toList();

        List<ForecastResponseDTO.MonthSummaryDTO> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String monthKey = year + "-" + String.format("%02d", m);
            String monthLabel = LocalDateTime.of(year, m, 1, 0, 0)
                    .getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));

            double received = 0, pending = 0;
            for (Map<String, List<FinanceEntity>> clientMonths : txByClientMonth.values()) {
                List<FinanceEntity> entries = clientMonths.getOrDefault(monthKey, List.of());
                received += entries.stream().filter(e -> e.getStatus() == FinanceStatusEnum.PAY).mapToDouble(FinanceEntity::getValue).sum();
                pending += entries.stream().filter(e -> e.getStatus() == FinanceStatusEnum.PENDING).mapToDouble(FinanceEntity::getValue).sum();
            }

            months.add(new ForecastResponseDTO.MonthSummaryDTO(monthKey, monthLabel, monthlyTotal, received, pending));
        }

        return new ForecastResponseDTO(clientDTOs, months, monthlyTotal, annualTotal, year);
    }
}
