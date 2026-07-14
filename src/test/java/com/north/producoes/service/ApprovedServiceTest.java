package com.north.producoes.service;

import com.north.producoes.controller.dto.request.ApproveRequestDTO;
import com.north.producoes.controller.dto.request.ApproveStatusUpdateRequestDTO;
import com.north.producoes.controller.dto.request.ApproveByPostRequestDTO;
import com.north.producoes.controller.dto.request.RejectByPostRequestDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private WhatsAppNotificationService whatsAppNotificationService;

    @InjectMocks
    private ApprovedService approvedService;

    @Nested
    @DisplayName("findByPostId() com usuário")
    class FindByPostIdShared {
        @Test
        @DisplayName("role USER lê aprovação de post de outro membro (board compartilhado)")
        void userReadsApprovalOfAnyPost() {
            UserEntity user = new UserEntity();
            user.setId(7L);
            user.setRole(UserRoleEnum.USER);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approval(5L, post(10L))));

            var result = approvedService.findByPostId(10L, user);

            assertThat(result.id()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {
        @Test
        @DisplayName("deve retornar aprovação quando id existir")
        void shouldReturnApprovalWhenIdExists() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            when(approveRepository.existsById(5L)).thenReturn(true);
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));

            // Act
            Optional<ApproveEntity> result = approvedService.findById(5L);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("deve lançar exceção quando id não existir")
        void shouldThrowExceptionWhenIdDoesNotExist() {
            // Arrange
            when(approveRepository.existsById(5L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> approvedService.findById(5L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Nenhum post encontrado com id: 5");
        }
    }

    @Nested
    @DisplayName("findApproveByStatus()")
    class FindApproveByStatus {
        @Test
        @DisplayName("deve retornar lista quando status existir")
        void shouldReturnListWhenStatusExists() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            when(approveRepository.findApproveEntitiesByStatus(ApproveStatusEnum.PENDING)).thenReturn(List.of(approve));

            // Act
            List<ApproveEntity> result = approvedService.findApproveByStatus(ApproveStatusEnum.PENDING);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getStatus()).isEqualTo(ApproveStatusEnum.PENDING);
        }

        @Test
        @DisplayName("deve lançar exceção quando status não existir")
        void shouldThrowExceptionWhenStatusDoesNotExist() {
            // Arrange
            when(approveRepository.findApproveEntitiesByStatus(ApproveStatusEnum.APPROVE)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> approvedService.findApproveByStatus(ApproveStatusEnum.APPROVE))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Nenhum post encontrado com status: APPROVE");
        }
    }

    @Nested
    @DisplayName("saveApprove()")
    class SaveApprove {
        @Test
        @DisplayName("deve salvar nova aprovação")
        void shouldSaveNewApproval() {
            // Arrange
            ApproveRequestDTO dto = new ApproveRequestDTO(10L, "public/posts/1/10/art.png", "art.png", "Legenda", null);
            PostEntity post = post(10L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(s3Service.isPublicKey(any())).thenReturn(true);
            when(approveRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ApproveEntity result = approvedService.saveApprove(dto);

            // Assert
            assertThat(result.getCaption()).isEqualTo("Legenda");
            assertThat(result.getStatus()).isEqualTo(ApproveStatusEnum.PENDING);
        }
    }

    @Nested
    @DisplayName("updateApprovalStatus()")
    class UpdateApprovalStatus {
        @Test
        @DisplayName("deve atualizar status e data de aprovação")
        void shouldUpdateStatusAndApprovedAt() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            ApproveStatusUpdateRequestDTO request = new ApproveStatusUpdateRequestDTO(ApproveStatusEnum.APPROVE, null);
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));
            when(approveRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            ApproveEntity result = approvedService.updateApprovalStatus(5L, request);

            // Assert
            assertThat(result.getStatus()).isEqualTo(ApproveStatusEnum.APPROVE);
            assertThat(result.getApprovedAt()).isNotNull();
            assertThat(result.getApprovedUser()).isEqualTo("system");
        }

        @Test
        @DisplayName("deve lançar exceção ao tentar mudar para PENDING")
        void shouldThrowExceptionWhenSettingToPending() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            when(approveRepository.findById(5L)).thenReturn(Optional.of(approve));

            // Act & Assert
            assertThatThrownBy(() -> approvedService.updateApprovalStatus(
                    5L,
                    new ApproveStatusUpdateRequestDTO(ApproveStatusEnum.PENDING, null)
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("APPROVE ou REJECTED");
        }
    }

    @Nested
    @DisplayName("internal methods")
    class InternalMethods {
        @Test
        @DisplayName("deve aprovar por post ID")
        void shouldApproveByPostId() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L));
            ApproveByPostRequestDTO request = new ApproveByPostRequestDTO(LocalDateTime.now().plusDays(2), "Notas");
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
            when(approveRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            var result = approvedService.approveByPostId(10L, "user", request);

            // Assert
            assertThat(result.status()).isEqualTo(ApproveStatusEnum.APPROVE);
            assertThat(result.approvedUser()).isEqualTo("user");
        }

        @Test
        @DisplayName("deve rejeitar por post ID e atualizar status do post")
        void shouldRejectByPostId() {
            // Arrange
            PostEntity post = post(10L);
            ApproveEntity approve = approval(5L, post);
            RejectByPostRequestDTO request = new RejectByPostRequestDTO("motivo");
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
            when(approveRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // Act
            var result = approvedService.rejectByPostId(10L, "user", request);

            // Assert
            assertThat(result.status()).isEqualTo(ApproveStatusEnum.REJECTED);
            assertThat(post.getStatus()).isEqualTo(PostStatusEnum.REJECTED);
            assertThat(result.rejectionReason()).isEqualTo("motivo");
        }
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
