package com.north.producoes.controller;

import com.north.producoes.controller.dto.request.MediaUploadCompleteRequestDTO;
import com.north.producoes.controller.dto.response.MediaUploadCompleteResponseDTO;
import com.north.producoes.controller.dto.response.MediaUrlResponseDTO;
import com.north.producoes.controller.dto.response.PresignedUploadResponseDTO;
import com.north.producoes.entity.UserEntity;
import com.north.producoes.entity.enums.UserRoleEnum;
import com.north.producoes.service.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("MediaController")
@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    @Test
    @DisplayName("deve gerar URL de upload")
    void shouldGenerateUploadUrl() {
        // Arrange
        UserEntity user = user();
        PresignedUploadResponseDTO serviceResponse = new PresignedUploadResponseDTO(
                "https://upload.example",
                "public/posts/1/10/art.png"
        );
        when(mediaService.generateUploadUrl(10L, "art.png", "image/png", user)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<PresignedUploadResponseDTO> response = mediaController.generateUploadUrl(10L, "art.png", "image/png", user);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
    }

    @Test
    @DisplayName("deve confirmar upload completo")
    void shouldMarkUploadComplete() {
        // Arrange
        UserEntity user = user();
        MediaUploadCompleteRequestDTO request = new MediaUploadCompleteRequestDTO(10L, "public/posts/1/10/art.png", "Art", null);
        MediaUploadCompleteResponseDTO serviceResponse = new MediaUploadCompleteResponseDTO(10L, "WAITING_APPROVAL", true);
        when(mediaService.markUploadComplete(request, user)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<MediaUploadCompleteResponseDTO> response = mediaController.markUploadComplete(request, user);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
    }

    @Test
    @DisplayName("deve retornar URL de preview")
    void shouldReturnPreviewUrl() {
        // Arrange
        UserEntity user = user();
        MediaUrlResponseDTO serviceResponse = new MediaUrlResponseDTO(5L, 10L, "https://cdn.example/art.png", List.of("https://cdn.example/art.png"), "Legenda");
        when(mediaService.getArtPreviewUrl(10L, user)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<MediaUrlResponseDTO> response = mediaController.getArtPreviewUrl(10L, user);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(serviceResponse);
    }

    @Test
    @DisplayName("deve buscar URL de preview da referência")
    void shouldGetReferencePreviewUrl() {
        // Arrange
        UserEntity user = user();
        MediaUrlResponseDTO response = new MediaUrlResponseDTO(null, 10L, "url", List.of("url"), "caption");
        when(mediaService.getReferencePreviewUrl(10L, user)).thenReturn(response);

        // Act
        ResponseEntity<MediaUrlResponseDTO> result = mediaController.getReferencePreviewUrl(10L, user);

        // Assert
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().mediaUrl()).isEqualTo("url");
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("User");
        user.setEmail("user@example.com");
        user.setRole(UserRoleEnum.USER);
        return user;
    }
}
