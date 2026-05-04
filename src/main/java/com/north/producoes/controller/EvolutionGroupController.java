package com.north.producoes.controller;

import com.north.producoes.controller.api.EvolutionGroupApi;
import com.north.producoes.controller.dto.request.EvolutionGroupLinkRequestDTO;
import com.north.producoes.controller.dto.response.EvolutionGroupResponseDTO;
import com.north.producoes.service.EvolutionApiService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class EvolutionGroupController implements EvolutionGroupApi {

    private final EvolutionApiService evolutionApiService;

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<EvolutionGroupResponseDTO>> findGroups() {
        return ResponseEntity.ok(evolutionApiService.findGroups());
    }

    @Override
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<EvolutionGroupResponseDTO> linkGroupToClient(
            @Valid EvolutionGroupLinkRequestDTO request) {
        return ResponseEntity.ok(evolutionApiService.linkGroupToClient(request));
    }
}
