package com.SaaS.Constructora.documents;

import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import com.SaaS.Constructora.documents.infrastructure.S3ObjectStorageAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el adaptador real contra LocalStack: direccionamiento path-style,
 * Signature V4, Subida simple, multipart, URL firmada y borrado.
 */
@Testcontainers
class S3ObjectStorageAdapterTest {

    private static final String BUCKET = "constructora-documents";

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.0.3"))
            .withServices(LocalStackContainer.Service.S3);

    private static S3Client client;
    private static S3Presigner presigner;
    private static S3ObjectStorageAdapter adapter;

    @BeforeAll
    static void setUp() {
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.getAccessKey(), localstack.getSecretKey()));
        var pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        URI endpoint = localstack.getEndpointOverride(LocalStackContainer.Service.S3);
        Region region = Region.of(localstack.getRegion());

        client = S3Client.builder()
                .endpointOverride(endpoint).region(region)
                .credentialsProvider(credentials).serviceConfiguration(pathStyle)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
        presigner = S3Presigner.builder()
                .endpointOverride(endpoint).region(region)
                .credentialsProvider(credentials).serviceConfiguration(pathStyle).build();
        client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        adapter = new S3ObjectStorageAdapter(client, presigner, BUCKET);
    }

    @AfterAll
    static void tearDown() {
        client.close();
        presigner.close();
    }

    @Test
    void guardaRecuperaFirmaYBorraUnObjeto() {
        String key = "tenants/t1/projects/p1/documents/d1/v1/planta.png";
        byte[] content = "contenido del plano".getBytes(StandardCharsets.UTF_8);

        adapter.store(key, new ByteArrayInputStream(content), content.length, "image/png");

        ResponseBytes<GetObjectResponse> stored = client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build());
        assertArrayEquals(content, stored.asByteArray());
        assertEquals("image/png", stored.response().contentType());

        String url = adapter.presignDownload(key, "planta.png");
        assertTrue(url.contains("/" + BUCKET + "/" + key), url);
        assertTrue(url.contains("X-Amz-Signature"), url);
        assertTrue(url.contains("response-content-disposition"), url);

        adapter.delete(key);
        assertThrows(NoSuchKeyException.class, () -> client.getObject(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()));
    }

    @Test
    void usaMultipartCuandoElArchivoSuperaElUmbral() {
        String key = "tenants/t1/projects/p1/documents/d2/v1/render.glb";
        byte[] content = new byte[(int) ObjectStoragePort.MULTIPART_THRESHOLD_BYTES + 1024];
        Arrays.fill(content, (byte) 7);

        adapter.store(key, new ByteArrayInputStream(content), content.length, "model/gltf-binary");

        ResponseBytes<GetObjectResponse> stored = client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build());
        assertArrayEquals(content, stored.asByteArray());
        adapter.delete(key);
    }
}
