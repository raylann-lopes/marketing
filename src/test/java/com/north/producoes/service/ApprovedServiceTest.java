package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveWhatsAppUpdateRequestDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ApprovedService")
@ExtendWith(MockitoExtension.class)
class ApprovedServiceTest {

    @Mock
    private ApproveRepository approveRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ApprovedService approvedService;

    @Nested
    @DisplayName("saveApprove()")
    class SaveApprove {

        @Test
        @DisplayName("deve criar aprovação quando post existe e chave S3 é pública")
        void shouldCreateApprovalWhenPostExistsAndS3KeyIsPublic() {
            // Arrange
            PostEntity post = post(10L);
            ApproveRequestDTO request = new ApproveRequestDTO(10L, " public/posts/1/10/art.png ", "art.png", "Legenda");
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(s3Service.isPublicKey("public/posts/1/10/art.png")).thenReturn(true);
            when(approveRepository.save(any(ApproveEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ApproveEntity result = approvedService.saveApprove(request);

            // Assert
            assertThat(result.getPost()).isSameAs(post);
            assertThat(result.getArtS3Key()).isEqualTo("public/posts/1/10/art.png");
            assertThat(result.getArtName()).isEqualTo("art.png");
            assertThat(result.getCaption()).isEqualTo("Legenda");
            assertThat(result.getApprovedUser()).isEmpty();
            verify(approveRepository).save(any(ApproveEntity.class));
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando chave S3 não for pública")
        void shouldThrowWhenS3KeyIsNotPublic() {
            // Arrange
            ApproveRequestDTO request = new ApproveRequestDTO(10L, "private/art.png", "art.png", "Legenda");
            when(postRepository.findById(10L)).thenReturn(Optional.of(post(10L)));
            when(s3Service.isPublicKey("private/art.png")).thenReturn(false);
            when(s3Service.getPublicPrefix()).thenReturn("public/posts");

            // Act & Assert
            assertThatThrownBy(() -> approvedService.saveApprove(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("public/posts");
            verify(approveRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("finders")
    class Finders {

        @Test
        @DisplayName("deve retornar aprovação por id quando existir")
        void shouldReturnApprovalById() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            when(approveRepository.existsById(5L)).thenReturn(true);
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));

            // Act
            Optional<ApproveEntity> result = approvedService.findById(5L);

            // Assert
            assertThat(result).contains(approve);
        }

        @Test
        @DisplayName("deve retornar aprovações por status")
        void shouldReturnApprovalsByStatus() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            when(approveRepository.findApproveEntitiesByStatus(ApproveStatusEnum.PENDING)).thenReturn(List.of(approve));

            // Act
            List<ApproveEntity> result = approvedService.findApproveByStatus(ApproveStatusEnum.PENDING);

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .extracting(ApproveEntity::getStatus)
                    .containsExactly(ApproveStatusEnum.PENDING);
        }

        @Test
        @DisplayName("deve retornar chave S3 da primeira aprovação do post")
        void shouldReturnS3KeyByPostId() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            approve.setArtS3Key("public/posts/1/10/art.png");
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));

            // Act
            String result = approvedService.getS3KeyByPostId(10L);

            // Assert
            assertThat(result).isEqualTo("public/posts/1/10/art.png");
        }
    }

    @Nested
    @DisplayName("updates")
    class Updates {

        @Test
        @DisplayName("deve atualizar metadados do WhatsApp")
        void shouldUpdateWhatsappMetadata() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            ApproveWhatsAppUpdateRequestDTO request = new ApproveWhatsAppUpdateRequestDTO("stanza-1", "sent-at", "ok", null);
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));
            when(approveRepository.save(approve)).thenReturn(approve);

            // Act
            ApproveEntity result = approvedService.updateWhatsappMetadata(5L, request);

            // Assert
            assertThat(result.getWhatsappStanzaId()).isEqualTo("stanza-1");
            assertThat(result.getWhatsappSentAt()).isEqualTo("sent-at");
            assertThat(result.getWhatsappResponseText()).isEqualTo("ok");
        }

        @Test
        @DisplayName("deve aprovar e preencher auditoria quando status for APPROVE")
        void shouldApproveAndSetAuditFields() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));
            when(approveRepository.save(approve)).thenReturn(approve);

            // Act
            ApproveEntity result = approvedService.updateApprovalStatus(
                    5L,
                    new ApproveStatusUpdateRequestDTO(ApproveStatusEnum.APPROVE)
            );

            // Assert
            assertThat(result.getStatus()).isEqualTo(ApproveStatusEnum.APPROVE);
            assertThat(result.getApprovedAt()).isNotNull();
            assertThat(result.getApprovedUser()).isEqualTo("n8n-callback");
        }

        @Test
        @DisplayName("deve rejeitar e limpar auditoria quando status for REJECT")
        void shouldRejectAndClearAuditFields() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            approve.setStatus(ApproveStatusEnum.APPROVE);
            approve.setApprovedUser("n8n-callback");
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));
            when(approveRepository.save(approve)).thenReturn(approve);

            // Act
            ApproveEntity result = approvedService.updateApprovalStatus(
                    5L,
                    new ApproveStatusUpdateRequestDTO(ApproveStatusEnum.REJECTED)
            );

            // Assert
            assertThat(result.getStatus()).isEqualTo(ApproveStatusEnum.REJECTED);
            assertThat(result.getApprovedAt()).isNull();
            assertThat(result.getApprovedUser()).isEmpty();
        }

        @Test
        @DisplayName("deve rejeitar status PENDING no endpoint interno")
        void shouldRejectPendingStatusForInternalEndpoint() {
            // Arrange
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approval(5L, post(10L))));

            // Act & Assert
            assertThatThrownBy(() -> approvedService.updateApprovalStatus(
                    5L,
                    new ApproveStatusUpdateRequestDTO(ApproveStatusEnum.PENDING)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("APPROVE ou REJECT");
        }
    }

    @Nested
    @DisplayName("deleteApproveById()")
    class DeleteApproveById {

        @Test
        @DisplayName("deve deletar aprovação quando existir")
        void shouldDeleteApprovalWhenExists() {
            // Arrange
            when(approveRepository.existsById(5L)).thenReturn(true);

            // Act
            approvedService.deleteApproveById(5L);

            // Assert
            verify(approveRepository).deleteById(5L);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando aprovação não existir")
        void shouldThrowWhenApprovalDoesNotExist() {
            // Arrange
            when(approveRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> approvedService.deleteApproveById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
            verify(approveRepository, never()).deleteById(99L);
        }
    }

    private static PostEntity post(Long id) {
        PostEntity post = new PostEntity();
        post.setId(id);
        return post;
    }

    private static ApproveEntity approval(Long id, PostEntity post) {
        ApproveEntity approve = new ApproveEntity();
        approve.setId(id);
        approve.setPost(post);
        approve.setArtS3Key("public/posts/1/10/art.png");
        approve.setArtName("art.png");
        approve.setCaption("Legenda");
        approve.setStatus(ApproveStatusEnum.PENDING);
        approve.setApprovedUser("");
        return approve;
    }
}
