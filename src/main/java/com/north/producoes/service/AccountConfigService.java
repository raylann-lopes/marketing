package com.north.producoes.service;

import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.AccountConfigRepository;
import com.north.producoes.repository.ClientRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AccountConfigService {

    private final AccountConfigRepository accountConfigRepository;
    private final ClientRepository clientRepository;

    /**
     * Configura o Instagram Account ID para um cliente.
     * Lança exceção se já existir configuração — mudanças exigem ação no banco.
     */
    @Transactional
    public AccountConfigEntity configure(AccountConfigRequestDTO dto, String adminEmail) {
        if (accountConfigRepository.existsByClientId(dto.clientId())) {
            throw new ResourceAlreadyExistsException(
                    "Configuração de conta já existe para o cliente ID " + dto.clientId()
                    + ". Para alterar, contate o administrador do sistema."
            );
        }

        ClientEntity client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente não encontrado com id: " + dto.clientId()));

        AccountConfigEntity config = new AccountConfigEntity();
        config.setClient(client);
        config.setInstagramAccountId(dto.instagramAccountId());
        config.setConfiguredBy(adminEmail);
        config.setConfiguredAt(LocalDateTime.now());

        return accountConfigRepository.save(config);
    }

    public AccountConfigEntity findByClientId(Long clientId) {
        return accountConfigRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma configuração de conta encontrada para o cliente ID " + clientId));
    }

    /** Usado internamente pelo MediaController para montar o payload do n8n. */
    public String getInstagramAccountId(Long clientId) {
        return findByClientId(clientId).getInstagramAccountId();
    }
}
