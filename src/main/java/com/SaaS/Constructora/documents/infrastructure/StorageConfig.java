package com.SaaS.Constructora.documents.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Clientes S3 del módulo documents. Neon inyecta las variables AWS_* al ejecutar
 * en la nube; en local {@code neon env pull} las escribe en .env/.env.local y
 * Spring las importa, por lo que se entregan explícitamente al SDK (su cadena
 * de credenciales por defecto no lee los PropertySource de Spring).
 * <p>
 * Neon Object Storage exige direccionamiento path-style y Signature V4.
 */
@Configuration
public class StorageConfig {

    @Bean
    S3Client s3Client(@Value("${app.storage.endpoint:}") String endpoint,
                      @Value("${app.storage.region:us-east-1}") String region,
                      @Value("${app.storage.access-key:}") String accessKey,
                      @Value("${app.storage.secret-key:}") String secretKey) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(s3Configuration())
                // Los proveedores S3-compatible (Neon, LocalStack) no aceptan los
                // checksums automáticos que el SDK añade por defecto en versiones recientes.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        applyEndpoint(builder::endpointOverride, endpoint);
        return builder.build();
    }

    @Bean
    S3Presigner s3Presigner(@Value("${app.storage.endpoint:}") String endpoint,
                            @Value("${app.storage.region:us-east-1}") String region,
                            @Value("${app.storage.access-key:}") String accessKey,
                            @Value("${app.storage.secret-key:}") String secretKey) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials(accessKey, secretKey))
                .serviceConfiguration(s3Configuration());
        applyEndpoint(builder::endpointOverride, endpoint);
        return builder.build();
    }

    private AwsCredentialsProvider credentials(String accessKey, String secretKey) {
        if (accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
        }
        return DefaultCredentialsProvider.create();
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }

    private void applyEndpoint(java.util.function.Consumer<URI> setter, String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            setter.accept(URI.create(endpoint.trim()));
        }
    }
}
