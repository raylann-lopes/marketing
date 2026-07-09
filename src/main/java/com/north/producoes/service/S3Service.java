package com.north.producoes.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
@Getter
@Setter
@Slf4j
public class S3Service {

    private static final String DEFAULT_PUBLIC_PREFIX = "public/posts";
    private static final String DEFAULT_REFERENCE_PREFIX = "public/references";

    private final S3Presigner presigner;
    private final S3Client s3Client;
    private final String bucket;
    private final String region;
    private final String publicPrefix;
    private final String publicBaseUrl;

    public S3Service(
            S3Presigner presigner,
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.public-prefix:" + DEFAULT_PUBLIC_PREFIX + "}") String publicPrefix,
            @Value("${aws.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.presigner = presigner;
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.region = region;
        this.publicPrefix = normalizePrefix(publicPrefix);
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    public String generateUploadUrl(String s3Key, String contentType) {
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

    public String buildPublicUploadKey(Long clientId, Long postId, String filename) {
        return String.format("%s/%d/%d/%s", publicPrefix, clientId, postId, sanitizeFilename(filename));
    }

    public String buildReferenceKey(Long clientId, Long postId, String filename) {
        return String.format("%s/references/%d/%d/%s", publicPrefix, clientId, postId, sanitizeFilename(filename));
    }

    public boolean isPublicKey(String s3Key) {
        if (!StringUtils.hasText(s3Key)) return false;
        String key = normalizeKey(s3Key);
        return key.startsWith(publicPrefix + "/");
    }

    /**
     * Detecta chaves de vídeo pela extensão. Carrossel no Instagram só aceita
     * imagens no fluxo atual (itens criados via image_url na Meta Graph API).
     */
    public boolean isVideoKey(String s3Key) {
        if (!StringUtils.hasText(s3Key)) return false;
        String key = s3Key.toLowerCase();
        return key.endsWith(".mp4") || key.endsWith(".mov")
                || key.endsWith(".webm") || key.endsWith(".avi") || key.endsWith(".mkv");
    }

    public String buildPublicUrl(String s3Key) {
        String normalizedKey = normalizeKey(s3Key);
        if (!isPublicKey(normalizedKey)) {
            throw new IllegalArgumentException("Chave fora do prefixo publico configurado: " + normalizedKey);
        }

        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl + "/" + normalizedKey;
        }

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, normalizedKey);
    }

    public String resolveReadUrl(String s3Key) {
        return buildPublicUrl(s3Key);
    }

    /**
     * Remove objetos do bucket em best-effort: falha na limpeza do S3 não pode
     * derrubar o fluxo de negócio (o registro já saiu do banco). Chaves fora
     * do prefixo público são ignoradas por segurança.
     */
    public void deleteObjects(Collection<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) return;

        List<ObjectIdentifier> targets = s3Keys.stream()
                .filter(this::isPublicKey)
                .map(this::normalizeKey)
                .distinct()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
        if (targets.isEmpty()) return;

        try {
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(targets).build())
                    .build());
            log.info("[S3] {} objeto(s) removido(s) do bucket.", targets.size());
        } catch (SdkException e) {
            log.warn("[S3] Falha ao remover {} objeto(s) — arquivos órfãos permanecem no bucket: {}",
                    targets.size(), e.getMessage());
        }
    }

    public String getPublicPrefix() {
        return publicPrefix;
    }

    private String normalizePrefix(String configuredPrefix) {
        String prefix = StringUtils.hasText(configuredPrefix) ? configuredPrefix.trim() : DEFAULT_PUBLIC_PREFIX;
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String normalizeBaseUrl(String configuredBaseUrl) {
        if (!StringUtils.hasText(configuredBaseUrl)) {
            return "";
        }
        String baseUrl = configuredBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String normalizeKey(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("s3Key e obrigatoria");
        }
        String key = s3Key.trim();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "upload.jpg";
        }

        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        String extension = "";
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot > 0 && lastDot < normalized.length() - 1) {
            extension = normalized.substring(lastDot).toLowerCase(Locale.ROOT);
            normalized = normalized.substring(0, lastDot);
        }

        String safeName = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-_]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        if (!StringUtils.hasText(safeName)) {
            safeName = "upload";
        }

        if (!StringUtils.hasText(extension)) {
            extension = ".jpg";
        }

        return safeName + extension;
    }
}
