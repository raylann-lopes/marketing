package com.north.producoes.controller;

import com.north.producoes.controller.api.AccountConfigApi;
import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.service.AccountConfigService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@AllArgsConstructor
public class AccountConfigController implements AccountConfigApi {

    private final AccountConfigService accountConfigService;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> configure(@Valid AccountConfigRequestDTO request, Principal principal) {
        AccountConfigResponseDTO response = AccountConfigResponseDTO.from(
                accountConfigService.configure(request, principal.getName())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> findByClientId(Long clientId) {
        return ResponseEntity.ok(AccountConfigResponseDTO.from(
                accountConfigService.findByClientId(clientId)
        ));
    }
}
