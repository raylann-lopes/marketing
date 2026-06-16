package com.north.producoes.controller;

import com.north.producoes.controller.api.ApifyContentIdeaApi;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.integration.apify.dto.ApifyContentIdeaSignalDTO;
import com.north.producoes.integration.apify.dto.ContentIdeaTermsDTO;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.service.ApifySearchContentIdeaService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AllArgsConstructor
@RestController("/api/apify")
public class ApifyContentIdeaController implements ApifyContentIdeaApi {

    private final ApifySearchContentIdeaService apifySearchContentIdeaService;
    private final ClientRepository clientRepository;

    @Override
    @PostMapping("/content-ideas/{id}")
    public ResponseEntity<ContentIdeaTermsDTO> getContentIdeas(@PathVariable Long id) {
        var clientId = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException ("Cliente nao encontrado" + id));
        var terms = apifySearchContentIdeaService.generateIdeas(clientId);
        return ResponseEntity.ok(terms);
    }

    @Override
    @PostMapping("/content-ideas/{id}/signals")
    public ResponseEntity<List<ApifyContentIdeaSignalDTO>> searchInstagramSignals(@PathVariable Long id) {
        var clientId = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException ("Cliente nao encontrado" + id));
        var signal = apifySearchContentIdeaService.searchInstagramSignals(clientId);
        return ResponseEntity.ok(signal);
    }

}
