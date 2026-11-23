package com.SaaS.Constructora.documents.infrastructure;

import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Adaptador S3 para Neon Object Storage (y cualquier proveedor S3-compatible). */
@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectStorageAdapter.class);
    private static final int PART_SIZE = 8 * 1024 * 1024;

    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ObjectStorageAdapter(S3Client client, S3Presigner presigner,
                                  @Value("${app.storage.bucket:constructora-documents}") String bucket) {
        this.client = client;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            if (size <= MULTIPART_THRESHOLD_BYTES) {
                client.putObject(PutObjectRequest.builder()
                                .bucket(bucket).key(key).contentType(contentType)
                                .contentLength(size).build(),
                        RequestBody.fromInputStream(content, size));
            } else {
                multipartUpload(key, content, size, contentType);
            }
        } catch (SdkException | IOException exception) {
            log.error("Fallo al almacenar el objeto {} en el bucket {}", key, bucket, exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El almacenamiento de archivos no está disponible", exception);
        }
    }

    @Override
    public String presignDownload(String key, String originalName) {
        String fileName = originalName == null ? "archivo" : originalName.replace("\"", "");
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition("attachment; filename=\"" + fileName + "\"")
                .build();
        return presigner.presignGetObject(builder -> builder
                        .signatureDuration(Duration.ofSeconds(PRESIGN_TTL_SECONDS))
                        .getObjectRequest(request))
                .url().toString();
    }

    @Override
    public void delete(String key) {
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException exception) {
            log.warn("No se pudo eliminar el objeto {} del bucket {}", key, bucket, exception);
        }
    }

    private void multipartUpload(String key, InputStream content, long size, String contentType)
            throws IOException {
        String uploadId = client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).build()).uploadId();
        List<CompletedPart> parts = new ArrayList<>();
        long remaining = size;
        int partNumber = 1;
        try {
            while (remaining > 0) {
                byte[] chunk = content.readNBytes((int) Math.min(PART_SIZE, remaining));
                if (chunk.length == 0) {
                    throw new IOException("El stream terminó antes del tamaño declarado");
                }
                var response = client.uploadPart(UploadPartRequest.builder()
                                .bucket(bucket).key(key).uploadId(uploadId)
                                .partNumber(partNumber).contentLength((long) chunk.length).build(),
                        RequestBody.fromBytes(chunk));
                parts.add(CompletedPart.builder().partNumber(partNumber).eTag(response.eTag()).build());
                remaining -= chunk.length;
                partNumber++;
            }
            client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(bucket).key(key).uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder().parts(parts).build())
                    .build());
        } catch (RuntimeException | IOException exception) {
            client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket).key(key).uploadId(uploadId).build());
            throw exception;
        }
    }
}
