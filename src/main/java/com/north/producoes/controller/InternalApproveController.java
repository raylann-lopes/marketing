package com.north.producoes.controller;

import com.north.producoes.controller.api.ApproveInternalApi;
import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.service.ApprovedService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/internal/approvals")
public class InternalApproveController implements ApproveInternalApi {

    private final ApprovedService approvedService;

    @Override
    public ResponseEntity<ApproveResponseDTO> updateWhatsappMetadata(
            @PathVariable Long id,
            @Valid @RequestBody ApproveWhatsAppUpdateRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.updateWhatsappMetadata(id, request)));
    }

    @Override
    public ResponseEntity<ApproveResponseDTO> updateApprovalStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApproveStatusUpdateRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.updateApprovalStatus(id, request)));
    }

    @Override
    public ResponseEntity<ApproveResponseDTO> findByWhatsappStanzaId(@RequestParam String stanzaId) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.findByStanzaId(stanzaId)));
    }
}
