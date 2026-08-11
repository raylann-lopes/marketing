package com.north.producoes.controller;

import com.north.producoes.controller.api.AccountConfigApi;
import com.north.producoes.controller.dto.request.AccountConfigRequestDTO;
import com.north.producoes.controller.dto.request.MetaAdsAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.service.AccountConfigService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/account-config")
public class AccountConfigController implements AccountConfigApi {

    private final AccountConfigService accountConfigService;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> configure(@Valid @RequestBody AccountConfigRequestDTO request, Principal principal) {
        AccountConfigResponseDTO response = AccountConfigResponseDTO.from(
                accountConfigService.configure(request, principal.getName())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> findByClientId(@PathVariable Long clientId) {
        return ResponseEntity.ok(AccountConfigResponseDTO.from(
                accountConfigService.findByClientId(clientId)
        ));
    }

    @Override
    @PatchMapping("/client/{clientId}/meta-ad-account")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> linkMetaAdAccount(
            @PathVariable Long clientId,
            @Valid @RequestBody MetaAdsAccountLinkRequestDTO request) {
        AccountConfigResponseDTO config = AccountConfigResponseDTO.from(
                accountConfigService.linkMetaAdAccount(clientId, request.metaAdAccountId())
        );
        return ResponseEntity.ok(config);
    }

    @Override
    @DeleteMapping("/client/{clientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<Void> deleteByClientId(@PathVariable Long clientId) {
        accountConfigService.deleteByClientId(clientId);
        return ResponseEntity.noContent().build();
    }
}
