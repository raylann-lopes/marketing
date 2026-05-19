package com.north.producoes.service;

import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.exception.ResourceNotFoundException;
import com.north.producoes.repository.ApproveRepository;
import com.north.producoes.repository.ClientRepository;
import com.north.producoes.repository.PostRepository;
import com.north.producoes.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("PostService")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OpenAiService openAiService;

    @Mock
    private ApproveRepository approveRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private PostService postService;

    @Nested
    @DisplayName("findByStatus()")
    class FindByStatus {

        @Test
        @DisplayName("deve retornar posts quando o status existe")
        void shouldReturnPostsWhenStatusExists() {
            // Arrange
            PostEntity post = new PostEntity();
            post.setStatus(PostStatusEnum.DEMAND);
            when(postRepository.findByStatus(PostStatusEnum.DEMAND)).thenReturn(List.of(post));

            // Act
            List<PostEntity> result = postService.findByStatus(PostStatusEnum.DEMAND);

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .extracting(PostEntity::getStatus)
                    .containsExactly(PostStatusEnum.DEMAND);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando não houver posts com o status")
        void shouldThrowWhenStatusHasNoPosts() {
            // Arrange
            when(postRepository.findByStatus(PostStatusEnum.POSTED)).thenReturn(List.of());

            // Act & Assert
            assertThatThrownBy(() -> postService.findByStatus(PostStatusEnum.POSTED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Nenhum post encontrado com status");
        }
    }

    @Nested
    @DisplayName("findByScheduledAt()")
    class FindByScheduledAt {

        @Test
        @DisplayName("deve retornar posts dentro do período informado")
        void shouldReturnPostsInsideScheduledPeriod() {
            // Arrange
            LocalDateTime start = LocalDateTime.of(2026, 4, 25, 9, 0);
            LocalDateTime end = LocalDateTime.of(2026, 4, 25, 18, 0);
            PostEntity post = new PostEntity();
            post.setScheduledAt(LocalDateTime.of(2026, 4, 25, 12, 0));
            when(postRepository.findByScheduledAtBetween(start, end)).thenReturn(List.of(post));

            // Act
            List<PostEntity> result = postService.findByScheduledAt(start, end);

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .extracting(PostEntity::getScheduledAt)
                    .containsExactly(LocalDateTime.of(2026, 4, 25, 12, 0));
        }

        @Test
        @DisplayName("deve lançar IllegalArgumentException quando data inicial for posterior à final")
        void shouldThrowWhenStartDateIsAfterEndDate() {
            // Arrange
            LocalDateTime start = LocalDateTime.of(2026, 4, 26, 9, 0);
            LocalDateTime end = LocalDateTime.of(2026, 4, 25, 18, 0);

            // Act & Assert
            assertThatThrownBy(() -> postService.findByScheduledAt(start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Data de inicio deve ser anterior");

            verify(postRepository, never()).findByScheduledAtBetween(any(), any());
        }
    }

    @Nested
    @DisplayName("savePost()")
    class SavePost {

        @Test
        @DisplayName("deve salvar post quando cliente e usuário existem")
        void shouldSavePostWhenClientAndUserExist() {
            // Arrange
            ClientEntity client = client(1L);
            UserEntity user = user(2L);
            PostRequestDTO request = postRequest(1L, 2L);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(postRepository.save(any(PostEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            PostEntity result = postService.savePost(request);

            // Assert
            assertThat(result)
                    .extracting(PostEntity::getTitle, PostEntity::getTheme, PostEntity::getObjective, PostEntity::getStatus)
                    .containsExactly("Calendario editorial", "Lancamento", "Gerar demanda", PostStatusEnum.DEMAND);
            assertThat(result.getScheduledAt()).isEqualTo(request.scheduledAt());
            assertThat(result.getClient()).isSameAs(client);
            assertThat(result.getUser()).isSameAs(user);

            ArgumentCaptor<PostEntity> postCaptor = ArgumentCaptor.forClass(PostEntity.class);
            verify(postRepository).save(postCaptor.capture());
            assertThat(postCaptor.getValue().getClient()).isSameAs(client);
            assertThat(postCaptor.getValue().getUser()).isSameAs(user);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando cliente não existe")
        void shouldThrowWhenClientDoesNotExist() {
            // Arrange
            PostRequestDTO request = postRequest(99L, 2L);
            when(clientRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> postService.savePost(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Cliente não encontrado com id: 99");

            verifyNoInteractions(userRepository);
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePost()")
    class UpdatePost {

        @Test
        @DisplayName("deve atualizar post quando post, cliente e usuário existem")
        void shouldUpdatePostWhenDependenciesExist() {
            // Arrange
            PostEntity existingPost = new PostEntity();
            existingPost.setId(10L);
            ClientEntity client = client(1L);
            UserEntity user = user(2L);
            PostRequestDTO request = postRequest(1L, 2L);

            when(postRepository.findById(10L)).thenReturn(Optional.of(existingPost));
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));
            when(postRepository.save(existingPost)).thenReturn(existingPost);

            // Act
            PostEntity result = postService.updatePost(10L, request);

            // Assert
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result)
                    .extracting(PostEntity::getTitle, PostEntity::getTheme, PostEntity::getObjective, PostEntity::getStatus)
                    .containsExactly("Calendario editorial", "Lancamento", "Gerar demanda", PostStatusEnum.DEMAND);
            assertThat(result.getClient()).isSameAs(client);
            assertThat(result.getUser()).isSameAs(user);
            verify(postRepository).save(existingPost);
        }
    }

    @Nested
    @DisplayName("deletePostById()")
    class DeletePostById {

        @Test
        @DisplayName("deve deletar aprovação vinculada antes de deletar post")
        void shouldDeleteApprovalBeforeDeletingPost() {
            // Arrange
            when(postRepository.existsById(10L)).thenReturn(true);

            // Act
            postService.deletePostById(10L);

            // Assert
            InOrder inOrder = inOrder(approveRepository, postRepository);
            inOrder.verify(approveRepository).deleteByPostId(10L);
            inOrder.verify(postRepository).deleteById(10L);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando post não existe")
        void shouldThrowWhenPostDoesNotExist() {
            // Arrange
            when(postRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> postService.deletePostById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Post nao encontrado com id: 99");

            verifyNoInteractions(approveRepository);
            verify(postRepository, never()).deleteById(99L);
        }
    }

    @Nested
    @DisplayName("generateCaption()")
    class GenerateCaption {

        @Test
        @DisplayName("deve gerar legenda usando arte manual sanitizada e atualizar aprovação existente")
        void shouldGenerateCaptionWithSanitizedManualArtAndUpdateExistingApproval() {
            // Arrange
            PostEntity post = post(10L);
            ApproveEntity approve = new ApproveEntity();
            approve.setPost(post);
            approve.setArtS3Key("old-art.png");

            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(approveRepository.findByPostId(10L)).thenReturn(List.of(approve));
            when(s3Service.resolveReadUrl("new-art.png")).thenReturn("https://cdn.example/new-art.png");
            when(openAiService.generateCaption(post, "https://cdn.example/new-art.png")).thenReturn("Legenda gerada");
            when(approveRepository.save(approve)).thenReturn(approve);

            // Act
            ApproveEntity result = postService.generateCaption(10L, "{artS3Key: \"new-art.png\"}");

            // Assert
            assertThat(result).isSameAs(approve);
            assertThat(result.getCaption()).isEqualTo("Legenda gerada");
            verify(s3Service).resolveReadUrl("new-art.png");
            verify(openAiService).generateCaption(post, "https://cdn.example/new-art.png");
            verify(approveRepository).save(approve);
        }

        @Test
        @DisplayName("deve retornar aprovação temporária quando ainda não existe aprovação")
        void shouldReturnTemporaryApprovalWhenApprovalDoesNotExist() {
            // Arrange
            PostEntity post = post(10L);
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(approveRepository.findByPostId(10L)).thenReturn(List.of());
            when(openAiService.generateCaption(post, null)).thenReturn("Legenda nova");

            // Act
            ApproveEntity result = postService.generateCaption(10L, null);

            // Assert
            assertThat(result.getPost()).isSameAs(post);
            assertThat(result.getCaption()).isEqualTo("Legenda nova");
            assertThat(result.getArtName()).isEqualTo("Legenda Gerada");
            assertThat(result.getStatus()).isEqualTo(ApproveStatusEnum.PENDING);
            assertThat(result.getArtS3Key()).isNull();
            verifyNoInteractions(s3Service);
            verify(approveRepository, never()).save(any());
        }
    }

    private static PostRequestDTO postRequest(Long clientId, Long userId) {
        return new PostRequestDTO(
                "Calendario editorial",
                "Lancamento",
                "Gerar demanda",
                PostStatusEnum.DEMAND,
                LocalDateTime.of(2026, 4, 25, 14, 0),
                clientId,
                userId,
                null,
                null,
                null
        );
    }

    private static PostEntity post(Long id) {
        PostEntity post = new PostEntity();
        post.setId(id);
        return post;
    }

    private static ClientEntity client(Long id) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        return client;
    }

    private static UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        return user;
    }
}
