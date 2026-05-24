package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceSchedulerService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final FinanceService financeService;

    // Executa às 00:00 do dia 1º de cada mês
    @Scheduled(cron = "0 0 0 1 * *")
    public void generateMonthlyEntriesForActiveClients() {
        List<ClientEntity> activeClients = clientRepository.findByStatus(ClientStatusEnum.ACTIVE);

        UserEntity systemUser = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRoleEnum.ADMIN)
                .findFirst()
                .orElse(null);

        if (systemUser == null) {
            log.warn("Scheduler: nenhum usuário ADMIN encontrado para gerar entradas mensais.");
            return;
        }

        int count = 0;
        for (ClientEntity client : activeClients) {
            try {
                financeService.generateCurrentMonthEntry(client, systemUser);
                count++;
            } catch (Exception e) {
                log.error("Scheduler: erro ao gerar entrada para cliente {} - {}", client.getName(), e.getMessage());
            }
        }

        log.info("Scheduler: {} entradas mensais geradas para o mês corrente.", count);
    }
}
