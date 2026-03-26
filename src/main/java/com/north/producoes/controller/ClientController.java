package com.north.producoes.controller;

import com.north.producoes.dto.request.ClientRequest;
import com.north.producoes.dto.response.ClientResponse;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.service.ClientService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@AllArgsConstructor
public class ClientController {
    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> findAll(){
        List<ClientResponse> clients = clientService.findAllClient()
                .stream()
                .map(ClientResponse::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ClientResponse> findByEmail(@PathVariable String email){
        return ResponseEntity.ok(ClientResponse.from(clientService.findByEmail(email)));
    }

    @GetMapping("/number/{number}")
    public ResponseEntity<ClientResponse> findByNumber(@PathVariable String number){
        return ResponseEntity.ok(ClientResponse.from(clientService.findByNumber(number)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ClientResponse>> findByStatus(@PathVariable ClientStatusEnum status){
        List<ClientResponse> clients = clientService.findByStatus(status)
                .stream()
                .map(ClientResponse::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> saveClient(@Valid @RequestBody ClientRequest request){
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

    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id){
        clientService.deleteClientById(id);
        return ResponseEntity.noContent().build();
    }
}
