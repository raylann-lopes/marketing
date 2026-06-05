package com.north.producoes.service;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("MediaService")
@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private ApproveRepository approveRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private WhatsAppNotificationService whatsAppNotificationService;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void initTxSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTxSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Nested
    @DisplayName("generateUploadUrl()")
    class GenerateUploadUrl {

        @Test
        @DisplayName("deve gerar URL de upload para usuário autorizado")
        void shouldGenerateUploadUrlForAuthorizedUser() {
            PostEntity post = post(10L, client(1L), user(2L, UserRoleEnum.USER));
            UserEntity user = user(2L, UserRoleEnum.USER);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(s3Service.buildPublicUploadKey(1L, 10L, "Arte Final.png"))
                    .thenReturn("public/posts/1/10/arte-final.png");
            when(s3Service.generateUploadUrl("public/posts/1/10/arte-final.png", "image/png"))
                    .thenReturn("https://upload.example");

            PresignedUploadResponseDTO result = mediaService.generateUploadUrl(10L, "Arte Final.png", "image/png", user);

            assertThat(result.uploadUrl()).isEqualTo("https://upload.example");
            assertThat(result.s3Key()).isEqualTo("public/posts/1/10/arte-final.png");
        }

        @Test
        @DisplayName("deve negar acesso quando usuário não estiver autenticado")
        void shouldDenyWhenUserIsNull() {
            when(postRepository.findById(10L)).thenReturn(Optional.of(post(10L, client(1L), user(2L, UserRoleEnum.USER))));

            assertThatThrownBy(() -> mediaService.generateUploadUrl(10L, "art.png", "image/png", null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("autenticado");
            verifyNoInteractions(s3Service);
        }
    }

    @Nested
    @DisplayName("getArtPreviewUrl()")
    class GetArtPreviewUrl {

        @Test
        @DisplayName("deve retornar URL de preview quando aprovação existir")
        void shouldReturnPreviewUrlWhenApprovalExists() {
            UserEntity admin = user(1L, UserRoleEnum.ADMIN);
            PostEntity post = post(10L, client(1L), user(2L, UserRoleEnum.USER));
            ApproveEntity approve = approval(5L, post, ApproveStatusEnum.PENDING);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
            when(s3Service.resolveReadUrl("public/posts/1/10/art.png")).thenReturn("https://cdn.example/art.png");

            MediaUrlResponseDTO result = mediaService.getArtPreviewUrl(10L, admin);

            assertThat(result.approvalId()).isEqualTo(5L);
            assertThat(result.mediaUrl()).isEqualTo("https://cdn.example/art.png");
            assertThat(result.caption()).isEqualTo("Legenda");
        }
    }

    @Nested
    @DisplayName("markUploadComplete()")
    class MarkUploadComplete {

        @Test
        @DisplayName("deve salvar aprovação, atualizar post e disparar notificação WhatsApp")
        void shouldSaveApprovalUpdatePostAndDispatchWhatsApp() {
            ClientEntity client = client(1L);
            UserEntity user = user(2L, UserRoleEnum.USER);
            PostEntity post = post(10L, client, user);
            MediaUploadCompleteRequestDTO request = new MediaUploadCompleteRequestDTO(
                    10L, " public/posts/1/10/art.png ", "Arte Final");

            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of());
            when(s3Service.isPublicKey("public/posts/1/10/art.png")).thenReturn(true);
            when(approveRepository.save(any(ApproveEntity.class))).thenAnswer(inv -> {
                ApproveEntity approve = inv.getArgument(0);
                approve.setId(5L);
                return approve;
            });
            when(postRepository.save(post)).thenReturn(post);
            when(s3Service.resolveReadUrl("public/posts/1/10/art.png")).thenReturn("https://cdn.example/art.png");

            MediaUploadCompleteResponseDTO result = mediaService.markUploadComplete(request, user);

            assertThat(result.postId()).isEqualTo(10L);
            assertThat(result.postStatus()).isEqualTo(PostStatusEnum.WAITING_APPROVAL.name());
            assertThat(result.webhookDispatched()).isTrue();
            assertThat(post.getStatus()).isEqualTo(PostStatusEnum.WAITING_APPROVAL);

            // Simula o commit da transação para disparar o afterCommit callback
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            // Verifica que WhatsApp só é acionado após o commit (sem N8N)
            verify(whatsAppNotificationService).sendApprovalRequest(
                    any(Long.class), any(Long.class), any(Long.class), any(String.class));
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando s3Key não estiver no prefixo público")
        void shouldThrowWhenS3KeyIsNotPublic() {
            UserEntity user = user(2L, UserRoleEnum.USER);
            PostEntity post = post(10L, client(1L), user);
            MediaUploadCompleteRequestDTO request = new MediaUploadCompleteRequestDTO(10L, "private/art.png", "Arte");
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of());
            when(s3Service.isPublicKey("private/art.png")).thenReturn(false);
            when(s3Service.getPublicPrefix()).thenReturn("public/posts");

            assertThatThrownBy(() -> mediaService.markUploadComplete(request, user))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("public/posts");
            verify(approveRepository, never()).save(any());
        }
    }

    private static ApproveEntity approval(Long id, PostEntity post, ApproveStatusEnum status) {
        ApproveEntity approve = new ApproveEntity();
        approve.setId(id);
        approve.setPost(post);
        approve.setArtS3Key("public/posts/1/10/art.png");
        approve.setArtName("art.png");
        approve.setCaption("Legenda");
        approve.setStatus(status);
        approve.setApprovedUser("");
        return approve;
    }

    private static PostEntity post(Long id, ClientEntity client, UserEntity user) {
        PostEntity post = new PostEntity();
        post.setId(id);
        post.setTitle("Titulo");
        post.setTheme("Tema");
        post.setObjective("Objetivo");
        post.setStatus(PostStatusEnum.DEMAND);
        post.setScheduledAt(LocalDateTime.of(2026, 5, 10, 10, 0));
        post.setClient(client);
        post.setUser(user);
        return post;
    }

    private static ClientEntity client(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName("Cliente");
        client.setNumber("(11) 99999-9999");
        client.setWhatsappGroupId("group-1");
        client.setWhatsappGroupName("Grupo Cliente");
        client.setNiche("Saude");
        client.setVoiceTone("Formal");
        client.setStatus(ClientStatusEnum.ACTIVE);
        return client;
    }

    private static UserEntity user(Long id, UserRoleEnum role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("User");
        user.setEmail("user@example.com");
        user.setRole(role);
        return user;
    }
}
