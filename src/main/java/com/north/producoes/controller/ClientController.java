package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ClientRequestDTO;
import com.north.producoes.controller.dto.request.ClientStatusRequestDTO;
import com.north.producoes.controller.dto.response.ClientResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.service.ClientService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@AllArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<ClientResponseDTO>> findAll() {
        List<ClientResponseDTO> clients = clientService.findAllClient()
                .stream()
                .map(ClientResponseDTO::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ClientResponseDTO> findByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.findByEmail(email)));
    }

    @GetMapping("/number/{number}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ClientResponseDTO> findByNumber(@PathVariable String number) {
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.findByNumber(number)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<ClientResponseDTO>> findByStatus(@PathVariable ClientStatusEnum status) {
        List<ClientResponseDTO> clients = clientService.findByStatus(status)
                .stream()
                .map(ClientResponseDTO::from)
                .toList();
        return ResponseEntity.ok(clients);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ClientResponseDTO> saveClient(
            @Valid @RequestBody ClientRequestDTO request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClientResponseDTO.from(clientService.saveClient(request, currentUser)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ClientResponseDTO> updateClient(@PathVariable Long id, @Valid @RequestBody ClientRequestDTO request) {
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.updateClient(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<ClientResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ClientStatusRequestDTO request,
            @AuthenticationPrincipal UserEntity currentUser) {
        return ResponseEntity.ok(ClientResponseDTO.from(clientService.updateStatus(id, request.status(), currentUser)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        clientService.deleteClientById(id);
        return ResponseEntity.noContent().build();
    }
}
