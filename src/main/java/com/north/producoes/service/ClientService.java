package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ClientRequestDTO;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.exception.ResourceAlreadyExistsException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ClientRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
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
    public ClientEntity saveClient(ClientRequestDTO request) {
        if (clientRepository.findByEmail(request.email()).isPresent()) {
            throw new ResourceAlreadyExistsException("Cliente com email " + request.email() + " ja cadastrado");
        } else if (clientRepository.findByNumber(request.number()).isPresent()) {
            throw new ResourceAlreadyExistsException("Cliente com numero " + request.number() + " ja cadastrado");
        }

        ClientEntity client = new ClientEntity();
        return getClientEntity(request, client);
    }

    @Transactional
    public ClientEntity updateClient(Long id, ClientRequestDTO request) {
        ClientEntity existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: id " + id));

        if (clientRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ResourceAlreadyExistsException("Cliente com email " + request.email() + " ja cadastrado");
        }
        if (clientRepository.existsByNumberAndIdNot(request.number(), id)) {
            throw new ResourceAlreadyExistsException("Cliente com numero " + request.number() + " ja cadastrado");
        }

        return getClientEntity(request, existingClient);
    }

    @Transactional
    public void deleteClientById(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cliente nao encontrado: id " + id);
        }
        clientRepository.deleteById(id);
    }

    @NotNull
    private ClientEntity getClientEntity(ClientRequestDTO request, ClientEntity client) {
        client.setName(request.name());
        client.setEmail(request.email());
        client.setNumber(request.number());
        client.setDriveLink(request.driveLink());
        client.setVoiceTone(request.voiceTone());
        client.setNiche(request.niche());
        return clientRepository.save(client);
    }
}
