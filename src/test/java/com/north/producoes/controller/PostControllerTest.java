package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.CaptionRequestDTO;
import com.north.producoes.controller.dto.request.PostRequestDTO;
import com.north.producoes.controller.dto.response.ApproveResponseDTO;
import com.north.producoes.controller.dto.response.PostResponseDTO;
import com.north.producoes.entity.ApproveEntity;
import com.north.producoes.entity.ClientEntity;
import com.north.producoes.entity.PostEntity;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.ApproveStatusEnum;
import com.north.producoes.entity.enums.PostStatusEnum;
import com.north.producoes.service.PostService;
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

@DisplayName("PostController")
@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    @Test
    @DisplayName("deve retornar todos os posts do board para a equipe")
    void shouldReturnAllPosts() {
        // Arrange
        when(postService.findAllPost()).thenReturn(List.of(post(10L)));

        // Act
        ResponseEntity<List<PostResponseDTO>> response = postController.findAll();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .hasSize(1)
                .extracting(PostResponseDTO::id)
                .containsExactly(10L);
    }

    @Test
    @DisplayName("deve criar post")
    void shouldCreatePost() {
        // Arrange
        PostRequestDTO request = request();
        when(postService.savePost(request)).thenReturn(post(10L));

        // Act
        ResponseEntity<PostResponseDTO> response = postController.savePost(request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("deve deletar post usando status atual do controller")
    void shouldDeletePostUsingControllerCurrentStatus() {
        // Act
        ResponseEntity<Void> response = postController.deletePostById(10L);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(postService).deletePostById(10L);
    }

    @Test
    @DisplayName("deve gerar legenda passando artS3Key informado")
    void shouldGenerateCaptionWithProvidedS3Key() {
        // Arrange
        CaptionRequestDTO request = new CaptionRequestDTO("public/posts/1/10/art.png");
        when(postService.generateCaption(10L, "public/posts/1/10/art.png")).thenReturn(approval(5L));

        // Act
        ResponseEntity<ApproveResponseDTO> response = postController.generateCaption(10L, request);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().caption()).isEqualTo("Legenda");
    }

    private static PostRequestDTO request() {
        return new PostRequestDTO(
                "Titulo",
                "Tema",
                "Objetivo",
                PostStatusEnum.DEMAND,
                LocalDateTime.of(2026, 5, 10, 10, 0),
                1L,
                2L,
                false,
                null,
                null,
                com.north.producoes.entity.enums.PostFormatEnum.IMAGE
        );
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

        UserEntity user = new UserEntity();
        user.setId(2L);

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
}
