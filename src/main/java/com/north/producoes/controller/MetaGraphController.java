package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.MetaInstagramAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramAccountResponseDTO;
import com.north.producoes.service.MetaGraphService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/meta")
@AllArgsConstructor
public class MetaGraphController {

    private final MetaGraphService metaGraphService;

    @GetMapping("/instagram-accounts")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<MetaInstagramAccountResponseDTO>> findInstagramAccounts() {
        return ResponseEntity.ok(metaGraphService.findInstagramAccounts());
    }

    @PostMapping("/instagram-accounts/link")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> linkInstagramAccount(
            @Valid @RequestBody MetaInstagramAccountLinkRequestDTO request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountConfigResponseDTO.from(
                        metaGraphService.linkInstagramAccount(request, principal.getName())
                ));
    }
}
