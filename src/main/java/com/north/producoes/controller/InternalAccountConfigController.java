package com.north.producoes.controller;

import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.service.AccountConfigService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint interno consumido pelo n8n para buscar credenciais de conta.
 * Protegido pelo InternalApiKeyFilter (X-Internal-Api-Key).
 *
 * n8n deve chamar GET /api/internal/account-config/{clientId} ao invés de
 * receber o accessToken no payload de publicação — padrão pull é mais seguro.
 */
@RestController
@RequestMapping("/api/internal/account-config")
@AllArgsConstructor
public class InternalAccountConfigController {

    private final AccountConfigService accountConfigService;

    @GetMapping("/{clientId}")
    public ResponseEntity<AccountConfigResponseDTO> findByClientId(@PathVariable Long clientId) {
        return ResponseEntity.ok(
                AccountConfigResponseDTO.from(accountConfigService.findByClientId(clientId))
        );
    }
}
