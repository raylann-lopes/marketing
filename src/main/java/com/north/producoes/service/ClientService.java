package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.repository.ClientRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    public List<ClientEntity> findAllClient() {
        return clientRepository.findAll();
    }

    public ClientEntity findByEmail(String email) {
        return clientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente nao encontrado"));
    }

    public ClientEntity findByNumber(String number) {
        return clientRepository.findByNumber(number)
                .orElseThrow(() -> new RuntimeException("Numero de telefone nao encontrado"));
    }

    public List<ClientEntity> findByStatus(ClientStatusEnum status) {
        List<ClientEntity> clientStatus = clientRepository.findByStatus(status);
        if (clientRepository.findByStatus(status).isEmpty()) {
            throw new RuntimeException("Cliente nao encontrado");
        }
        return clientStatus;
    }

    @Transactional
    public ClientEntity saveClient(ClientEntity client){
        if (clientRepository.findByEmail(client.getEmail()).isPresent()) {
            throw new RuntimeException("Cliente ja cadastrado");
        }
        return clientRepository.save(client);
    }

    @Transactional
    public ClientEntity updateClient(ClientEntity client) {
        if (clientRepository.findById(client.getId()).isEmpty()) {
            throw new RuntimeException("Cliente nao encontrado");
        }
        return clientRepository.save(client);
    }

    @Transactional
    public void deleteClientById(Long id) {
        clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nao foi possivel deletar, cliente nao encontrado"));
        clientRepository.deleteById(id);
    }
}

