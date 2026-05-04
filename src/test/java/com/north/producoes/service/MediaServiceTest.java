package com.north.producoes.service;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.AccountConfigEntity;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.ClientStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.exception.AiIntegrationException;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private AccountConfigService accountConfigService;

    @Mock
    private ApproveRepository approveRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private N8nWebhookService n8nWebhookService;

    @InjectMocks
    private MediaService mediaService;

    @Nested
    @DisplayName("generateUploadUrl()")
    class GenerateUploadUrl {

        @Test
        @DisplayName("deve gerar URL de upload para usuário autorizado")
        void shouldGenerateUploadUrlForAuthorizedUser() {
            // Arrange
            PostEntity post = post(10L, client(1L), user(2L, UserRoleEnum.USER));
            UserEntity user = user(2L, UserRoleEnum.USER);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(s3Service.buildPublicUploadKey(1L, 10L, "Arte Final.png"))
                    .thenReturn("public/posts/1/10/arte-final.png");
            when(s3Service.generateUploadUrl("public/posts/1/10/arte-final.png", "image/png"))
                    .thenReturn("https://upload.example");

            // Act
            PresignedUploadResponseDTO result = mediaService.generateUploadUrl(10L, "Arte Final.png", "image/png", user);

            // Assert
            assertThat(result.uploadUrl()).isEqualTo("https://upload.example");
            assertThat(result.s3Key()).isEqualTo("public/posts/1/10/arte-final.png");
        }

        @Test
        @DisplayName("deve negar acesso quando usuário não estiver autenticado")
        void shouldDenyWhenUserIsNull() {
            // Arrange
            when(postRepository.findById(10L)).thenReturn(Optional.of(post(10L, client(1L), user(2L, UserRoleEnum.USER))));

            // Act & Assert
            assertThatThrownBy(() -> mediaService.generateUploadUrl(10L, "art.png", "image/png", null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("nao autenticado");
            verifyNoInteractions(s3Service);
        }
    }

    @Nested
    @DisplayName("getArtPreviewUrl()")
    class GetArtPreviewUrl {

        @Test
        @DisplayName("deve retornar URL de preview quando aprovação existir")
        void shouldReturnPreviewUrlWhenApprovalExists() {
            // Arrange
            UserEntity admin = user(1L, UserRoleEnum.ADMIN);
            PostEntity post = post(10L, client(1L), user(2L, UserRoleEnum.USER));
            ApproveEntity approve = approval(5L, post, ApproveStatusEnum.PENDING);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
            when(s3Service.resolveReadUrl("public/posts/1/10/art.png")).thenReturn("https://cdn.example/art.png");

            // Act
            MediaUrlResponseDTO result = mediaService.getArtPreviewUrl(10L, admin);

            // Assert
            assertThat(result.approvalId()).isEqualTo(5L);
            assertThat(result.mediaUrl()).isEqualTo("https://cdn.example/art.png");
            assertThat(result.caption()).isEqualTo("Legenda");
        }
    }

    @Nested
    @DisplayName("getMediaUrlForN8n()")
    class GetMediaUrlForN8n {

        @Test
        @DisplayName("deve retornar mídia e credenciais quando aprovação estiver aprovada")
        void shouldReturnMediaAndCredentialsWhenApproved() {
            // Arrange
            ClientEntity client = client(1L);
            PostEntity post = post(10L, client, user(2L, UserRoleEnum.USER));
            ApproveEntity approve = approval(5L, post, ApproveStatusEnum.APPROVE);
            AccountConfigEntity config = accountConfig(client, "1784140000", "access-token");
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(s3Service.resolveReadUrl("public/posts/1/10/art.png")).thenReturn("https://cdn.example/art.png");
            when(accountConfigService.findByClientId(1L)).thenReturn(config);

            // Act
            MediaUrlResponseDTO result = mediaService.getMediaUrlForN8n(10L);

            // Assert
            assertThat(result.mediaUrl()).isEqualTo("https://cdn.example/art.png");
            assertThat(result.igUserId()).isEqualTo("1784140000");
            assertThat(result.accessToken()).isEqualTo("access-token");
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando aprovação ainda não estiver aprovada")
        void shouldThrowWhenApprovalIsNotApproved() {
            // Arrange
            ApproveEntity approve = approval(5L, post(10L, client(1L), user(2L, UserRoleEnum.USER)), ApproveStatusEnum.PENDING);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));

            // Act & Assert
            assertThatThrownBy(() -> mediaService.getMediaUrlForN8n(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ainda não aprovado");
            verify(postRepository, never()).findById(10L);
        }
    }

    @Nested
    @DisplayName("markUploadComplete()")
    class MarkUploadComplete {

        @Test
        @DisplayName("deve salvar aprovação, atualizar post e disparar webhook")
        void shouldSaveApprovalUpdatePostAndDispatchWebhook() {
            // Arrange
            ClientEntity client = client(1L);
            UserEntity user = user(2L, UserRoleEnum.USER);
            PostEntity post = post(10L, client, user);
            AccountConfigEntity config = accountConfig(client, "1784140000", "token");
            MediaUploadCompleteRequestDTO request = new MediaUploadCompleteRequestDTO(
                    10L,
                    " public/posts/1/10/art.png ",
                    "Arte Final"
            );

            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of());
            when(s3Service.isPublicKey("public/posts/1/10/art.png")).thenReturn(true);
            when(approveRepository.save(any(ApproveEntity.class))).thenAnswer(invocation -> {
                ApproveEntity approve = invocation.getArgument(0);
                approve.setId(5L);
                return approve;
            });
            when(postRepository.save(post)).thenReturn(post);
            when(accountConfigService.findByClientId(1L)).thenReturn(config);
            when(s3Service.resolveReadUrl("public/posts/1/10/art.png")).thenReturn("https://cdn.example/art.png");
            when(n8nWebhookService.dispatchArtUploadCompleted(any())).thenReturn(true);

            // Act
            MediaUploadCompleteResponseDTO result = mediaService.markUploadComplete(request, user);

            // Assert
            assertThat(result.postId()).isEqualTo(10L);
            assertThat(result.postStatus()).isEqualTo(PostStatusEnum.WAITING_APPROVAL.name());
            assertThat(result.webhookDispatched()).isTrue();
            assertThat(post.getStatus()).isEqualTo(PostStatusEnum.WAITING_APPROVAL);

            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.captor();
            verify(n8nWebhookService).dispatchArtUploadCompleted(payloadCaptor.capture());
            assertThat(payloadCaptor.getValue())
                    .containsEntry("event", "ART_UPLOAD_COMPLETED")
                    .containsEntry("igUserId", "1784140000");
            @SuppressWarnings("unchecked")
            Map<String, Object> clientPayload = (Map<String, Object>) payloadCaptor.getValue().get("client");
            assertThat(clientPayload)
                    .containsEntry("whatsappGroupId", "group-1")
                    .containsEntry("whatsappGroupName", "Grupo Cliente");
        }

        @Test
        @DisplayName("deve lançar AiIntegrationException quando webhook falhar")
        void shouldThrowWhenWebhookDispatchFails() {
            // Arrange
            ClientEntity client = client(1L);
            UserEntity user = user(2L, UserRoleEnum.USER);
            PostEntity post = post(10L, client, user);
            MediaUploadCompleteRequestDTO request = new MediaUploadCompleteRequestDTO(10L, "public/posts/1/10/art.png", "Arte");

            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of());
            when(s3Service.isPublicKey("public/posts/1/10/art.png")).thenReturn(true);
            when(approveRepository.save(any(ApproveEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(postRepository.save(post)).thenReturn(post);
            when(accountConfigService.findByClientId(1L)).thenReturn(accountConfig(client, "1784140000", "token"));
            when(s3Service.resolveReadUrl("public/posts/1/10/art.png")).thenReturn("https://cdn.example/art.png");
            when(n8nWebhookService.dispatchArtUploadCompleted(any())).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> mediaService.markUploadComplete(request, user))
                    .isInstanceOf(AiIntegrationException.class)
                    .hasMessageContaining("webhook");
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando s3Key não estiver no prefixo público")
        void shouldThrowWhenS3KeyIsNotPublic() {
            // Arrange
            UserEntity user = user(2L, UserRoleEnum.USER);
            PostEntity post = post(10L, client(1L), user);
            MediaUploadCompleteRequestDTO request = new MediaUploadCompleteRequestDTO(10L, "private/art.png", "Arte");
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postRepository.existsByIdAndUserId(10L, 2L)).thenReturn(true);
            when(approveRepository.findByPostId(10L)).thenReturn(List.of());
            when(s3Service.isPublicKey("private/art.png")).thenReturn(false);
            when(s3Service.getPublicPrefix()).thenReturn("public/posts");

            // Act & Assert
            assertThatThrownBy(() -> mediaService.markUploadComplete(request, user))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("public/posts");
            verify(approveRepository, never()).save(any());
        }
    }

    private static AccountConfigEntity accountConfig(ClientEntity client, String igUserId, String accessToken) {
        AccountConfigEntity config = new AccountConfigEntity();
        config.setClient(client);
        config.setIgUserId(igUserId);
        config.setAccessToken(accessToken);
        return config;
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
