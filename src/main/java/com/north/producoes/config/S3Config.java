package com.north.producoes.config;

import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key:}")
    private String secretAccessKey;

    // Vazio = AWS real (comportamento inalterado). Setado = qualquer storage
    // S3-compatible (ex.: MinIO) — usado na migração pra self-hosted.
    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    // MinIO/self-hosted normalmente exige path-style; AWS real usa
    // virtual-hosted-style por padrão, então o default aqui é false.
    @Value("${aws.s3.path-style-access-enabled:false}")
    private boolean pathStyleAccessEnabled;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(serviceConfiguration());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(serviceConfiguration());
        }
        return builder.build();
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .build();
    }

    private AwsCredentialsProvider credentialsProvider() {
        if (StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey)) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    accessKeyId,
                    secretAccessKey
            ));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
