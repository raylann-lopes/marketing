package com.north.producoes.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.function.Predicate;

@Service
public class S3Service {

    private final S3Presigner presigner;
    private final String bucket;

    public S3Service(S3Presigner presigner, @Value("${aws.s3.bucket}") String bucket) {
        this.presigner = presigner;
        this.bucket = bucket;
    }

    /**
     * Gera uma URL presigned para upload direto do frontend para o S3.
     * O frontend faz PUT para essa URL com o arquivo; o backend nunca toca o binário.
     * Expira em 15 minutos.
     * Inclui CORS headers para que o navegador consiga fazer o preflight OPTIONS.
     *
     * @param s3Key       chave do objeto, ex.: "posts/42/101/arte.png"
     * @param contentType MIME type do arquivo
     */
    public String generateUploadUrl(String s3Key, String contentType) {
        Predicate<String> allowedHeaders = h ->
            h.equalsIgnoreCase("Content-Type") || h.equalsIgnoreCase("x-amz-*");

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putRequest)
                .build();

        return presigner.presignPutObject(presignRequest).url().toString();
    }

    /**
     * Gera uma URL presigned para leitura — repassada pelo n8n à Meta API.
     * A Meta API precisa de tempo suficiente para processar; expira em 1 hora.
     *
     * @param s3Key chave do objeto armazenada em ApproveEntity.artS3Key
     */
    public String generateDownloadUrl(String s3Key) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Monta a chave S3 padrão para uma arte de post.
     * Formato: posts/{clientId}/{postId}/{filename}
     */
    public static String buildS3Key(Long clientId, Long postId, String filename) {
        return String.format("posts/%d/%d/%s", clientId, postId, filename);
    }
}
