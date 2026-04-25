package com.north.producoes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.text.Normalizer;
import java.time.Duration;
import java.util.Locale;

@Service
public class S3Service {

    private static final String DEFAULT_PUBLIC_PREFIX = "public/posts";

    private final S3Presigner presigner;
    private final String bucket;
    private final String region;
    private final String publicPrefix;
    private final String publicBaseUrl;

    public S3Service(
            S3Presigner presigner,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.public-prefix:" + DEFAULT_PUBLIC_PREFIX + "}") String publicPrefix,
            @Value("${aws.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.region = region;
        this.publicPrefix = normalizePrefix(publicPrefix);
        this.publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
    }

    /**
     * Gera uma URL presigned para upload direto do frontend para o S3.
     * O frontend faz PUT para essa URL com o arquivo; o backend nunca toca o binário.
     * Expira em 15 minutos.
     * Inclui CORS headers para que o navegador consiga fazer o preflight OPTIONS.
     *
     * @param s3Key       chave do objeto, ex.: "public/posts/42/101/arte.png"
     * @param contentType MIME type do arquivo
     */
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

    /**
     * Para uploads novos, o arquivo já deve nascer em prefixo público.
     * Assim o PUT continua protegido por URL presigned, mas o GET pode ser público/estável.
     */
    public String buildPublicUploadKey(Long clientId, Long postId, String filename) {
        return String.format("%s/%d/%d/%s", publicPrefix, clientId, postId, sanitizeFilename(filename));
    }

    public boolean isPublicKey(String s3Key) {
        return StringUtils.hasText(s3Key) && normalizeKey(s3Key).startsWith(publicPrefix + "/");
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
