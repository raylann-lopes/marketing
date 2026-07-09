package com.north.producoes.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("S3Service")
class S3ServiceTest {

    @Test
    @DisplayName("deve gerar chave pública sanitizando nome do arquivo")
    void shouldBuildPublicUploadKeyWithSanitizedFilename() {
        // Arrange
        S3Service s3Service = service(" public/posts/ ", "");

        // Act
        String result = s3Service.buildPublicUploadKey(1L, 10L, "Arte Final çã.PNG");

        // Assert
        assertThat(result).isEqualTo("public/posts/1/10/arte-final-ca.png");
    }

    @Test
    @DisplayName("deve identificar apenas chaves do prefixo público")
    void shouldIdentifyOnlyPublicKeys() {
        // Arrange
        S3Service s3Service = service("public/posts", "");

        // Act & Assert
        assertThat(s3Service.isPublicKey(" public/posts/1/10/art.png ")).isTrue();
        assertThat(s3Service.isPublicKey("private/posts/1/10/art.png")).isFalse();
    }

    @Test
    @DisplayName("deve criar URL pública com base customizada")
    void shouldBuildPublicUrlWithCustomBaseUrl() {
        // Arrange
        S3Service s3Service = service("public/posts", "https://cdn.example/");

        // Act
        String result = s3Service.buildPublicUrl("/public/posts/1/10/art.png");

        // Assert
        assertThat(result).isEqualTo("https://cdn.example/public/posts/1/10/art.png");
    }

    @Test
    @DisplayName("deve rejeitar URL pública fora do prefixo configurado")
    void shouldRejectPublicUrlOutsideConfiguredPrefix() {
        // Arrange
        S3Service s3Service = service("public/posts", "");

        // Act & Assert
        assertThatThrownBy(() -> s3Service.buildPublicUrl("private/posts/1/10/art.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prefixo publico");
    }

    private static S3Service service(String publicPrefix, String publicBaseUrl) {
        return new S3Service(
                mock(S3Presigner.class),
                mock(S3Client.class),
                "bucket",
                "sa-east-1",
                publicPrefix,
                publicBaseUrl
        );
    }
}
