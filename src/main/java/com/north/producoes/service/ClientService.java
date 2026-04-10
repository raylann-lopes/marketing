package com.north.producoes.service;

import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + email));
    }

    public ClientEntity findByNumber(String number) {
        return clientRepository.findByNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Numero de telefone nao encontrado: " + number));
    }

    public List<ClientEntity> findByStatus(ClientStatusEnum status) {
        List<ClientEntity> clientStatus = clientRepository.findByStatus(status);
        if (clientStatus.isEmpty()) {
            throw new ResourceNotFoundException("Cliente com status " + status + " nao encontrado!");
        }
        return clientStatus;
    }

    @Transactional
    public ClientEntity saveClient(ClientEntity client){
        if (clientRepository.findByEmail(client.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("Cliente com email" + client.getEmail() + "já cadastado");
        }
        return clientRepository.save(client);
    }

    @Transactional
    public ClientEntity updateClient(ClientEntity client) {
        if (clientRepository.findById(client.getId()).isEmpty()) {
            throw new ResourceNotFoundException("Cliente nao encontrado: id" + client.getId());
        }
        return clientRepository.save(client);
    }

    @Transactional
    public void deleteClientById(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente nao encontrado: id " + id);
        }
        clientRepository.deleteById(id);
    }
}

