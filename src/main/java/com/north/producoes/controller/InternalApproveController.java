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

@RestController
@AllArgsConstructor
@RequestMapping("/api/internal/approvals")
public class InternalApproveController {

    private final ApprovedService approvedService;

    @PatchMapping("/{id}/whatsapp")
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

    @GetMapping("/whatsapp")
    public ResponseEntity<ApproveResponseDTO> findByWhatsappStanzaId(@RequestParam String stanzaId) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.findByStanzaId(stanzaId)));
    }

    @PatchMapping("/post/{postId}/approve")
    public ResponseEntity<ApproveResponseDTO> internalApprove(
            @PathVariable Long postId,
            @RequestBody(required = false) InternalApprovalRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.internalApproveByPostId(postId, "admin", request)));
    }

    @PatchMapping("/post/{postId}/reject")
    public ResponseEntity<ApproveResponseDTO> internalReject(@PathVariable Long postId, @Valid @RequestBody InternalApprovalRejectRequestDTO request) {
        return ResponseEntity.ok(ApproveResponseDTO.from(approvedService.internalRejectByPostId(postId, "n8n-whatsapp", request.rejectionReason())));
    }
}
