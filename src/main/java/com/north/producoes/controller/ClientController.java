package com.north.producoes.controller;

import com.north.producoes.controller.api.ClientApi;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.controller.dto.request.ClientRequestDTO;
import com.north.producoes.controller.dto.response.ClientResponseDTO;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.service.ClientService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class ClientController implements ClientApi {

    private final ClientService clientService;

    @Override
    public ResponseEntity<List<ClientResponseDTO>> findAll() {
        List<ClientResponseDTO> clients = clientService.findAllClient()
                .stream()
                .map(ClientResponseDTO::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @Override
    public ResponseEntity<ClientResponseDTO> findByEmail(String email) {
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.findByEmail(email)));
    }

    @Override
    public ResponseEntity<ClientResponseDTO> findByNumber(String number) {
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.findByNumber(number)));
    }

    @Override
    public ResponseEntity<List<ClientResponseDTO>> findByStatus(ClientStatusEnum status) {
        List<ClientResponseDTO> clients = clientService.findByStatus(status)
                .stream()
                .map(ClientResponseDTO::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponseDTO> saveClient(ClientRequestDTO request) {
        ClientEntity client = new ClientEntity();
        client.setName(request.name());
        client.setEmail(request.email());
        client.setNumber(request.number());
        client.setDriveLink(request.driveLink());
        client.setVoiceTone(request.voiceTone());
        client.setNiche(request.niche());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClientResponseDTO.from(clientService.saveClient(client)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientResponseDTO> updateClient(Long id, ClientRequestDTO request) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setNumber(request.number());
        client.setDriveLink(request.driveLink());
        client.setVoiceTone(request.voiceTone());
        client.setNiche(request.niche());
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.updateClient(client)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteById(Long id) {
        clientService.deleteClientById(id);
        return ResponseEntity.noContent().build();
    }
}
