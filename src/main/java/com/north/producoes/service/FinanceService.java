package com.north.producoes.service;

import com.north.producoes.controller.dto.request.FinanceRequestDTO;
import com.north.producoes.controller.dto.response.ForecastResponseDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.FinanceRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public List<FinanceEntity> findAll() {
        return financeRepository.findAll();
    }

    public List<FinanceEntity> findByStatus(FinanceStatusEnum status) {
        return financeRepository.findByStatus(status);
    }

    public List<FinanceEntity> findByClientId(Long id) {
        return financeRepository.findByClientId(id);
    }

    public List<FinanceEntity> findByType(FinanceTypeEnum type) {
        return financeRepository.findByType(type);
    }

    @Transactional
    public FinanceEntity saveFinance(FinanceRequestDTO dto, UserEntity currentUser) {
        ClientEntity client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: id " + dto.clientId()));

        FinanceEntity entity = toEntity(dto, client, currentUser);
        FinanceEntity saved = financeRepository.save(entity);

        if (entity.getType() == FinanceTypeEnum.FIXED_EXPENSE || entity.getType() == FinanceTypeEnum.FIXED_REVENUE) {
            List<FinanceEntity> replicas = new ArrayList<>();
            for (int i = 1; i < 12; i++) {
                FinanceEntity replica = new FinanceEntity();
                replica.setClient(client);
                replica.setUser(currentUser);
                replica.setDescription(entity.getDescription());
                replica.setValue(entity.getValue());
                replica.setStatus(FinanceStatusEnum.PENDING);
                replica.setType(entity.getType());
                replica.setExpirationDate(entity.getExpirationDate().plusMonths(i));
                replica.setPaymentDate(entity.getPaymentDate().plusMonths(i));
                replicas.add(replica);
            }
            financeRepository.saveAll(replicas);
        }

        return saved;
    }

    @Transactional
    public FinanceEntity updateFinance(Long id, FinanceRequestDTO dto) {
        FinanceEntity existing = financeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada: id " + id));

        ClientEntity client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: id " + dto.clientId()));

        existing.setClient(client);
        existing.setDescription(dto.description());
        existing.setValue(dto.value());
        existing.setStatus(dto.status() != null ? dto.status() : existing.getStatus());
        existing.setType(dto.type());
        existing.setExpirationDate(dto.expirationDate().atStartOfDay());
        existing.setPaymentDate(
                dto.paymentDate() != null
                        ? dto.paymentDate().atStartOfDay()
                        : dto.expirationDate().atStartOfDay()
        );

        return financeRepository.save(existing);
    }

    @Transactional
    public void deleteFinanceById(Long id) {
        if (!financeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Conta nao encontrada: id " + id);
        }
        financeRepository.deleteById(id);
    }

    @Transactional
    public void generateCurrentMonthEntry(ClientEntity client, UserEntity createdBy) {
        if (client.getMonthlyValue() == null || client.getMonthlyValue().compareTo(BigDecimal.ZERO) <= 0) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = startOfMonth(now);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        boolean exists = !financeRepository.findByClientIdAndExpirationDateBetween(
                client.getId(), monthStart, monthEnd).isEmpty();
        if (exists) return;

        String monthLabel = now.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
        LocalDateTime dueDate = monthStart.plusDays(4);

        FinanceEntity entry = new FinanceEntity();
        entry.setClient(client);
        entry.setUser(createdBy);
        entry.setDescription("Mensalidade " + client.getName() + " - " + monthLabel + " " + now.getYear());
        entry.setValue(client.getMonthlyValue());
        entry.setStatus(FinanceStatusEnum.PENDING);
        entry.setType(FinanceTypeEnum.FIXED_REVENUE);
        entry.setExpirationDate(dueDate);
        entry.setPaymentDate(dueDate);
        financeRepository.save(entry);
    }

    @Transactional
    public void updatePendingEntriesValue(Long clientId, BigDecimal newValue) {
        LocalDateTime startOfNextMonth = startOfMonth(LocalDateTime.now()).plusMonths(1);
        List<FinanceEntity> pending = financeRepository.findByClientIdAndStatusAndExpirationDateAfter(
                clientId, FinanceStatusEnum.PENDING, startOfNextMonth.minusSeconds(1));
        for (FinanceEntity entry : pending) {
            entry.setValue(newValue);
        }
        financeRepository.saveAll(pending);
    }

    @Transactional
    public void removeUpcomingPendingEntries(Long clientId) {
        LocalDateTime endOfCurrentMonth = startOfMonth(LocalDateTime.now()).plusMonths(1).minusSeconds(1);
        financeRepository.deleteByClientIdAndStatusAndExpirationDateAfter(
                clientId, FinanceStatusEnum.PENDING, endOfCurrentMonth);
    }

    public ForecastResponseDTO getForecast(int year) {
        List<ClientEntity> allClients = clientRepository.findAll().stream()
                .filter(c -> c.getMonthlyValue() != null && c.getMonthlyValue().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        List<ClientEntity> activeClients = allClients.stream()
                .filter(c -> c.getStatus() == ClientStatusEnum.ACTIVE)
                .toList();

        LocalDateTime yearStart = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        LocalDateTime yearEnd = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<FinanceEntity> yearTransactions = financeRepository.findByExpirationDateBetween(yearStart, yearEnd);

        Map<Long, Map<String, List<FinanceEntity>>> txByClientMonth = yearTransactions.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getClient().getId(),
                        Collectors.groupingBy(e -> {
                            LocalDateTime d = e.getExpirationDate();
                            return d.getYear() + "-" + String.format("%02d", d.getMonthValue());
                        })
                ));

        BigDecimal monthlyTotal = activeClients.stream()
                .map(ClientEntity::getMonthlyValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal annualTotal = monthlyTotal.multiply(BigDecimal.valueOf(12));

        List<ClientEntity> visibleClients = allClients.stream()
                .filter(c -> c.getStatus() == ClientStatusEnum.ACTIVE || txByClientMonth.containsKey(c.getId()))
                .toList();

        List<ForecastResponseDTO.ClientForecastDTO> clientDTOs = visibleClients.stream()
                .map(c -> {
                    boolean isActive = c.getStatus() == ClientStatusEnum.ACTIVE;
                    Map<String, ForecastResponseDTO.ClientMonthDTO> monthData = new LinkedHashMap<>();
                    Map<String, List<FinanceEntity>> clientMonths = txByClientMonth.getOrDefault(c.getId(), Map.of());

                    for (int m = 1; m <= 12; m++) {
                        String monthKey = year + "-" + String.format("%02d", m);
                        List<FinanceEntity> entries = clientMonths.getOrDefault(monthKey, List.of());

                        BigDecimal received = sumByStatus(entries, FinanceStatusEnum.PAY);
                        BigDecimal pending = sumByStatus(entries, FinanceStatusEnum.PENDING);
                        BigDecimal expected = entries.isEmpty()
                                ? (isActive ? c.getMonthlyValue() : BigDecimal.ZERO)
                                : received.add(pending);

                        if (entries.isEmpty() && !isActive) continue;
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

            BigDecimal received = BigDecimal.ZERO;
            BigDecimal pending = BigDecimal.ZERO;
            for (Map<String, List<FinanceEntity>> clientMonths : txByClientMonth.values()) {
                List<FinanceEntity> entries = clientMonths.getOrDefault(monthKey, List.of());
                received = received.add(sumByStatus(entries, FinanceStatusEnum.PAY));
                pending = pending.add(sumByStatus(entries, FinanceStatusEnum.PENDING));
            }

            months.add(new ForecastResponseDTO.MonthSummaryDTO(monthKey, monthLabel, monthlyTotal, received, pending));
        }

        return new ForecastResponseDTO(clientDTOs, months, monthlyTotal, annualTotal, year);
    }

    private BigDecimal sumByStatus(List<FinanceEntity> entries, FinanceStatusEnum status) {
        return entries.stream()
                .filter(e -> e.getStatus() == status)
                .map(FinanceEntity::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime startOfMonth(LocalDateTime ref) {
        return ref.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private FinanceEntity toEntity(FinanceRequestDTO dto, ClientEntity client, UserEntity user) {
        FinanceEntity entity = new FinanceEntity();
        entity.setClient(client);
        entity.setUser(user);
        entity.setDescription(dto.description());
        entity.setValue(dto.value());
        entity.setStatus(dto.status() != null ? dto.status() : FinanceStatusEnum.PENDING);
        entity.setType(dto.type());
        entity.setExpirationDate(dto.expirationDate().atStartOfDay());
        entity.setPaymentDate(
                dto.paymentDate() != null
                        ? dto.paymentDate().atStartOfDay()
                        : dto.expirationDate().atStartOfDay()
        );
        return entity;
    }
}
