package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.EvolutionGroupLinkRequestDTO;
import com.north.producoes.controller.dto.response.EvolutionGroupResponseDTO;
import com.north.producoes.service.EvolutionApiService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/evolution")
@AllArgsConstructor
public class EvolutionGroupController {

    private final EvolutionApiService evolutionApiService;

    @GetMapping("/groups")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<List<EvolutionGroupResponseDTO>> findGroups() {
        return ResponseEntity.ok(evolutionApiService.findGroups());
    }

    @PostMapping("/groups/link")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ADMIN')")
    public ResponseEntity<EvolutionGroupResponseDTO> linkGroupToClient(
            @Valid @RequestBody EvolutionGroupLinkRequestDTO request) {
        return ResponseEntity.ok(evolutionApiService.linkGroupToClient(request));
    }
}
