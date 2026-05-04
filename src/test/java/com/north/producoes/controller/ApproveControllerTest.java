package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.repository.ApproveRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ApproveController")
@ExtendWith(MockitoExtension.class)
class ApproveControllerTest {

    @Mock
    private ApproveRepository approveRepository;

    @Mock
    private ApprovedService approvedService;

    @InjectMocks
    private ApproveController approveController;

    @Test
    @DisplayName("deve retornar todas as aprovações")
    void shouldReturnAllApprovals() {
        // Arrange
        when(approveRepository.findAll()).thenReturn(List.of(approval(5L)));

        // Act
        ResponseEntity<List<ApproveResponseDTO>> response = approveController.findAll();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .hasSize(1)
                .extracting(ApproveResponseDTO::id)
                .containsExactly(5L);
    }

    @Test
    @DisplayName("deve retornar 404 quando aprovação por post não existir")
    void shouldReturnNotFoundWhenApprovalByPostDoesNotExist() {
        // Arrange
        when(approveRepository.findByPostId(10L)).thenReturn(List.of());

        // Act
        ResponseEntity<ApproveResponseDTO> response = approveController.findByPostId(10L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("deve aprovar post existente")
    void shouldApproveExistingPost() {
        // Arrange
        ApproveEntity approve = approval(5L);
        when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
        when(approveRepository.save(approve)).thenReturn(approve);

        // Act
        ResponseEntity<ApproveResponseDTO> response = approveController.approvePost(10L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approve.getStatus()).isEqualTo(ApproveStatusEnum.APPROVE);
    }

    @Test
    @DisplayName("deve criar aprovação com status 201")
    void shouldCreateApprovalWithCreatedStatus() {
        // Arrange
        ApproveRequestDTO request = new ApproveRequestDTO(10L, "public/posts/1/10/art.png", "art.png", "Legenda");
        when(approvedService.saveApprove(request)).thenReturn(approval(5L));

        // Act
        ResponseEntity<ApproveResponseDTO> response = approveController.saveApprove(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("deve deletar aprovação com status 204")
    void shouldDeleteApprovalWithNoContentStatus() {
        // Act
        ResponseEntity<Void> response = approveController.deleteApproveById(5L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(approvedService).deleteApproveById(5L);
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
