package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.FinanceEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.entity.enums.FinanceStatusEnum;
import com.north.producoes.entity.enums.FinanceTypeEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.FinanceRepository;
import com.north.producoes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceSchedulerService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final FinanceRepository financeRepository;

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void generateMonthlyEntriesForActiveClients() {
        UserEntity systemUser = userRepository.findFirstByRole(UserRoleEnum.ADMIN).orElse(null);

        if (systemUser == null) {
            log.warn("Scheduler: nenhum usuário ADMIN encontrado. Entradas mensais NÃO geradas.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = startOfMonth(now);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

        // Batch: 1 query para saber quais clientes já têm entrada no mês (evita N+1)
        Set<Long> clientsWithEntry = financeRepository.findClientIdsWithEntryInMonth(monthStart, monthEnd);

        List<ClientEntity> activeClients = clientRepository.findByStatus(ClientStatusEnum.ACTIVE);

        String monthLabel = now.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));

        List<FinanceEntity> toCreate = activeClients.stream()
                .filter(c -> !clientsWithEntry.contains(c.getId()))
                .filter(c -> c.getMonthlyValue() != null && c.getMonthlyValue().compareTo(BigDecimal.ZERO) > 0)
                .map(c -> buildEntry(c, systemUser, monthStart, monthLabel, now.getYear()))
                .toList();

        if (!toCreate.isEmpty()) {
            financeRepository.saveAll(toCreate);
        }

        log.info("Scheduler: {} entradas mensais geradas para {}/{}.", toCreate.size(), monthLabel, now.getYear());
    }

    private FinanceEntity buildEntry(ClientEntity client, UserEntity user,
                                     LocalDateTime monthStart, String monthLabel, int year) {
        LocalDateTime dueDate = monthStart.plusDays(4);
        FinanceEntity entry = new FinanceEntity();
        entry.setClient(client);
        entry.setUser(user);
        entry.setDescription("Mensalidade " + client.getName() + " - " + monthLabel + " " + year);
        entry.setValue(client.getMonthlyValue());
        entry.setStatus(FinanceStatusEnum.PENDING);
        entry.setType(FinanceTypeEnum.FIXED_REVENUE);
        entry.setExpirationDate(dueDate);
        entry.setPaymentDate(dueDate);
        return entry;
    }

    private LocalDateTime startOfMonth(LocalDateTime ref) {
        return ref.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
}
