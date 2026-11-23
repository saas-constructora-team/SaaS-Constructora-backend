package com.SaaS.Constructora.documents.interfaces;

import com.SaaS.Constructora.documents.application.DocumentService;
import com.SaaS.Constructora.documents.application.ports.FileUpload;
import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** API REST de carga, versionado y consulta de documentos técnicos (HU-04). */
@RestController
@RequestMapping("/api/projects/{projectId}/documents")
public class DocumentController {

    private final DocumentService service;

    DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentResponse> upload(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @RequestPart("metadata") @Valid UploadDocumentRequest metadata,
            @RequestPart("file") MultipartFile file) {
        Document document = service.upload(user.tenantId(), user.userId(), projectId,
                metadata.toCommand(), toFileUpload(file));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentVersionResponse> addVersion(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID documentId,
            @RequestPart("metadata") @Valid AddVersionRequest metadata,
            @RequestPart("file") MultipartFile file) {
        DocumentVersion version = service.addVersion(user.tenantId(), user.userId(), projectId,
                documentId, metadata.toCommand(), toFileUpload(file));
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentVersionResponse.from(version));
    }

    @GetMapping
    List<DocumentResponse> list(@CurrentUser AuthenticatedUser user, @PathVariable UUID projectId) {
        return service.list(user.tenantId(), user.userId(), projectId).stream()
                .map(DocumentResponse::from).toList();
    }

    @GetMapping("/{documentId}/versions")
    List<DocumentVersionResponse> versions(@CurrentUser AuthenticatedUser user,
                                           @PathVariable UUID projectId,
                                           @PathVariable UUID documentId) {
        return service.versions(user.tenantId(), user.userId(), projectId, documentId).stream()
                .map(DocumentVersionResponse::from).toList();
    }

    @GetMapping("/{documentId}/versions/{versionId}/download")
    ResponseEntity<Void> download(@CurrentUser AuthenticatedUser user,
                                  @PathVariable UUID projectId,
                                  @PathVariable UUID documentId,
                                  @PathVariable UUID versionId) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION,
                        service.download(user.tenantId(), user.userId(), projectId,
                                documentId, versionId).url())
                .build();
    }

    private FileUpload toFileUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
        }
        try {
            return new FileUpload(file.getInputStream(), file.getOriginalFilename(),
                    file.getContentType(), file.getSize());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo leer el archivo", exception);
        }
    }
}
