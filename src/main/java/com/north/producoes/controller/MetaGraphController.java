package com.north.producoes.controller;

import com.north.producoes.controller.api.MetaGraphApi;
import com.north.producoes.controller.dto.request.MetaInstagramAccountLinkRequestDTO;
import com.north.producoes.controller.dto.response.AccountConfigResponseDTO;
import com.north.producoes.controller.dto.response.MetaInstagramAccountResponseDTO;
import com.north.producoes.service.MetaGraphService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@AllArgsConstructor
public class MetaGraphController implements MetaGraphApi {

    private final MetaGraphService metaGraphService;

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<MetaInstagramAccountResponseDTO>> findInstagramAccounts() {
        return ResponseEntity.ok(metaGraphService.findInstagramAccounts());
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<AccountConfigResponseDTO> linkInstagramAccount(
            @Valid MetaInstagramAccountLinkRequestDTO request,
            Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AccountConfigResponseDTO.from(
                        metaGraphService.linkInstagramAccount(request, principal.getName())
                ));
    }
}
