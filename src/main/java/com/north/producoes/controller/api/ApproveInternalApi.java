package com.north.producoes.controller.api;

import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.controller.dto.request.InternalApprovalRejectRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApproveInternalApi {
    @Operation(summary = "Atualiza os metadados de WhatsApp de uma aprovação (uso interno)")
    @ApiResponse(responseCode = "200", description = "Metadados de WhatsApp atualizados com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @ApiResponse(responseCode = "409", description = "Violação de integridade de dados")
    @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    @PatchMapping("/{id}/whatsapp")
    ResponseEntity<ApproveResponseDTO> updateWhatsappMetadata(@PathVariable Long id,
                                                              @Valid @RequestBody ApproveWhatsAppUpdateRequestDTO request);

    @Operation(summary = "Atualiza status da aprovação (APPROVE/REJECTED) para callback interno")
    @ApiResponse(responseCode = "200", description = "Status atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @ApiResponse(responseCode = "422", description = "Status inválido para este endpoint")
    @PatchMapping("/{id}/status")
    ResponseEntity<ApproveResponseDTO> updateApprovalStatus(@PathVariable Long id,
                                                            @Valid @RequestBody ApproveStatusUpdateRequestDTO request);

    @Operation(summary = "Busca aprovação por WhatsApp stanza ID (uso interno)")
    @ApiResponse(responseCode = "200", description = "Aprovação encontrada com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    @GetMapping("/whatsapp")
    ResponseEntity<ApproveResponseDTO> findByWhatsappStanzaId(@RequestParam String stanzaId);

    @Operation(summary = "Aprova um post internamente (uso pelo n8n)")
    @ApiResponse(responseCode = "200", description = "Post aprovado com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @PatchMapping("/post/{postId}/approve")
    ResponseEntity<ApproveResponseDTO> internalApprove(@PathVariable Long postId);

    @Operation(summary = "Rejeita um post internamente (uso pelo n8n)")
    @ApiResponse(responseCode = "200", description = "Post rejeitado com sucesso")
    @ApiResponse(responseCode = "404", description = "Aprovação não encontrada")
    @PatchMapping("/post/{postId}/reject")
    ResponseEntity<ApproveResponseDTO> internalReject(@PathVariable Long postId,
                                                      @Valid @RequestBody InternalApprovalRejectRequestDTO request);
}
