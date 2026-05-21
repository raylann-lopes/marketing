package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.controller.dto.request.InternalApprovalRejectRequestDTO;
import com.north.producoes.controller.dto.request.InternalApprovalRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.service.ApprovedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("InternalApproveController")
@ExtendWith(MockitoExtension.class)
class InternalApproveControllerTest {

    @Mock
    private ApprovedService approvedService;

    @InjectMocks
    private InternalApproveController internalApproveController;

    @Test
    @DisplayName("deve atualizar metadados do WhatsApp")
    void shouldUpdateWhatsappMetadata() {
        // Arrange
        ApproveWhatsAppUpdateRequestDTO request = new ApproveWhatsAppUpdateRequestDTO("stanza", "sent");
        ApproveEntity approve = approval(5L);
        approve.setWhatsappStanzaId("stanza");
        when(approvedService.updateWhatsappMetadata(5L, request)).thenReturn(approve);

        // Act
        ResponseEntity<ApproveResponseDTO> response = internalApproveController.updateWhatsappMetadata(5L, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().whatsappStanzaId()).isEqualTo("stanza");
    }

    @Test
    @DisplayName("deve aprovar internamente por post ID")
    void shouldInternalApprove() {
        // Arrange
        InternalApprovalRequestDTO request = new InternalApprovalRequestDTO(LocalDateTime.now().plusDays(1), "Notas");
        ApproveEntity approve = approval(5L);
        approve.setStatus(ApproveStatusEnum.APPROVE);
        when(approvedService.internalApproveByPostId(10L, "admin", request)).thenReturn(approve);

        // Act
        ResponseEntity<ApproveResponseDTO> response = internalApproveController.internalApprove(10L, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(ApproveStatusEnum.APPROVE);
    }

    @Test
    @DisplayName("deve rejeitar internamente por post ID")
    void shouldInternalReject() {
        // Arrange
        InternalApprovalRejectRequestDTO request = new InternalApprovalRejectRequestDTO("Motivo");
        ApproveEntity approve = approval(5L);
        approve.setStatus(ApproveStatusEnum.REJECTED);
        when(approvedService.internalRejectByPostId(10L, "n8n-whatsapp", "Motivo")).thenReturn(approve);

        // Act
        ResponseEntity<ApproveResponseDTO> response = internalApproveController.internalReject(10L, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(ApproveStatusEnum.REJECTED);
    }

    @Test
    @DisplayName("deve atualizar status de aprovação")
    void shouldUpdateApprovalStatus() {
        // Arrange
        ApproveStatusUpdateRequestDTO request = new ApproveStatusUpdateRequestDTO(ApproveStatusEnum.APPROVE);
        ApproveEntity approve = approval(5L);
        approve.setStatus(ApproveStatusEnum.APPROVE);
        when(approvedService.updateApprovalStatus(5L, request)).thenReturn(approve);

        // Act
        ResponseEntity<ApproveResponseDTO> response = internalApproveController.updateApprovalStatus(5L, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ApproveStatusEnum.APPROVE);
    }

    @Test
    @DisplayName("deve buscar aprovação por stanzaId")
    void shouldFindByWhatsappStanzaId() {
        // Arrange
        ApproveEntity approve = approval(5L);
        approve.setWhatsappStanzaId("stanza");
        when(approvedService.findByStanzaId("stanza")).thenReturn(approve);

        // Act
        ResponseEntity<ApproveResponseDTO> response = internalApproveController.findByWhatsappStanzaId("stanza");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(5L);
    }

    private static ApproveEntity approval(Long id) {
        ApproveEntity approve = new ApproveEntity();
        approve.setId(id);
        approve.setPost(post(10L));
        approve.setArtS3Key("public/posts/1/10/art.png");
        approve.setArtName("art.png");
        approve.setCaption("Legenda");
        approve.setStatus(ApproveStatusEnum.PENDING);
        approve.setApprovedUser("");
        return approve;
    }

    private static PostEntity post(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(1L);
        PostEntity post = new PostEntity();
        post.setId(id);
        post.setTitle("Titulo");
        post.setTheme("Tema");
        post.setObjective("Objetivo");
        post.setStatus(PostStatusEnum.DEMAND);
        post.setScheduledAt(LocalDateTime.of(2026, 5, 10, 10, 0));
        post.setClient(client);
        return post;
    }
}
