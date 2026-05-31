package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.controller.dto.request.InternalApprovalRejectRequestDTO;
import com.north.producoes.controller.dto.request.InternalApprovalRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.service.ApprovedService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints internos para gerenciamento de aprovações.
 * Protegido pelo InternalApiKeyFilter (X-Internal-Api-Key)
 * OU acesso de usuário ADMIN autenticado via JWT.
 */
@RestController
@RequestMapping("/api/internal/approvals")
@AllArgsConstructor
public class InternalApproveController {

    private final ApprovedService approvedService;

    @PatchMapping("/{id}/whatsapp-metadata")
    public ResponseEntity<ApproveResponseDTO> updateWhatsappMetadata(
            @PathVariable Long id,
            @Valid @RequestBody ApproveWhatsAppUpdateRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.updateWhatsappMetadata(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApproveResponseDTO> updateApprovalStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApproveStatusUpdateRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.updateApprovalStatus(id, request)));
    }

    @GetMapping("/whatsapp-stanza/{stanzaId}")
    public ResponseEntity<ApproveResponseDTO> findByWhatsappStanzaId(@PathVariable String stanzaId) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.findByStanzaId(stanzaId)));
    }

    @PatchMapping("/post/{postId}/approve")
    public ResponseEntity<ApproveResponseDTO> internalApprove(
            @PathVariable Long postId,
            @RequestBody(required = false) InternalApprovalRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(
                approvedService.internalApproveByPostId(postId, "admin", request)));
    }

    @PatchMapping("/post/{postId}/reject")
    public ResponseEntity<ApproveResponseDTO> internalReject(
            @PathVariable Long postId,
            @Valid @RequestBody InternalApprovalRejectRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(
                approvedService.internalRejectByPostId(postId, "admin", request.rejectionReason())));
    }
}
