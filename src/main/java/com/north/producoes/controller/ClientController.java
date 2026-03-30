package com.north.producoes.controller;

import com.north.producoes.controller.api.ClientApi;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.dto.request.ClientRequest;
import com.north.producoes.entity.dto.response.ClientResponse;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.service.ClientService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class ClientController implements ClientApi {

    private final ClientService clientService;

    @Override
    public ResponseEntity<List<ClientResponse>> findAll() {
        List<ClientResponse> clients = clientService.findAllClient()
                .stream()
                .map(ClientResponse::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @Override
    public ResponseEntity<ClientResponse> findByEmail(String email) {
        return ResponseEntity.ok(ClientResponse.from(clientService.findByEmail(email)));
    }

    @Override
    public ResponseEntity<ClientResponse> findByNumber(String number) {
        return ResponseEntity.ok(ClientResponse.from(clientService.findByNumber(number)));
    }

    @Override
    public ResponseEntity<List<ClientResponse>> findByStatus(ClientStatusEnum status) {
        List<ClientResponse> clients = clientService.findByStatus(status)
                .stream()
                .map(ClientResponse::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @Override
    public ResponseEntity<ClientResponse> saveClient(ClientRequest request) {
        ClientEntity client = new ClientEntity();
        client.setName(request.name());
        client.setEmail(request.email());
        client.setNumber(request.number());
        client.setDriveLink(request.driveLink());
        client.setVoiceTone(request.voiceTone());
        client.setNiche(request.niche());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClientResponse.from(clientService.saveClient(client)));
    }

    @Override
    public ResponseEntity<ClientResponse> updateClient(Long id, ClientRequest request) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setNumber(request.number());
        client.setDriveLink(request.driveLink());
        client.setVoiceTone(request.voiceTone());
        client.setNiche(request.niche());
        return ResponseEntity.ok(ClientResponse.from(clientService.updateClient(client)));
    }

    @Override
    public ResponseEntity<Void> deleteById(Long id) {
        clientService.deleteClientById(id);
        return ResponseEntity.noContent().build();
    }
}
