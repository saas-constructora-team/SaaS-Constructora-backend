package com.SaaS.Constructora.documents.application;

import com.SaaS.Constructora.documents.application.ports.DocumentRepository;
import com.SaaS.Constructora.documents.application.ports.DocumentTenantPort;
import com.SaaS.Constructora.documents.application.ports.FileUpload;
import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import com.SaaS.Constructora.documents.domain.AssetKind;
import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;
import com.SaaS.Constructora.documents.domain.DocumentVersionChange;
import com.SaaS.Constructora.documents.domain.FilePolicy;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.shared.events.DocumentVersionUploaded;
import com.SaaS.Constructora.shared.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Casos de uso del módulo documents (HU-04). El tenant se toma del contexto
 * autenticado, el proyecto se valida mediante el contrato público de projects
 * y el binario se delega al puerto de almacenamiento.
 */
@Service
public class DocumentService implements DocumentTenantPort {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final Pattern VERSION_LABEL = Pattern.compile("[A-Za-z0-9._-]{1,20}");
    private static final int HEADER_BYTES = 16;
    /** Número de binarios conservados en S3 por documento, incluida la versión vigente. */
    private static final int VERSION_RETENTION = 3;

    private final DocumentRepository repository;
    private final ObjectStoragePort storage;
    private final ProjectTenantPort projectTenantPort;
    private final ApplicationEventPublisher events;

    public DocumentService(DocumentRepository repository, ObjectStoragePort storage,
                           ProjectTenantPort projectTenantPort, ApplicationEventPublisher events) {
        this.repository = repository;
        this.storage = storage;
        this.projectTenantPort = projectTenantPort;
        this.events = events;
    }

    @Transactional
    public Document upload(UUID tenantId, UUID userId, UUID projectId,
                           UploadDocumentCommand command, FileUpload file) {
        Role role = currentRole();
        requireCanWrite(tenantId, projectId, userId, role);
        String label = requireVersionLabel(command.versionLabel());
        if (command.kind() == null || command.title() == null || command.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título y tipo de archivo son obligatorios");
        }

        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        String key = objectKey(tenantId, projectId, documentId, versionId, versionId, file.originalName());
        try {
            DocumentVersion version = storeVersion(tenantId, projectId, userId, documentId,
                    versionId, command.kind(), label, file, key);
            Document document = Document.create(documentId, tenantId, projectId, command.kind(),
                    command.title(), version.id(), userId);
            repository.save(document);
            repository.saveVersion(version);
            repository.recordChange(change(tenantId, documentId, version, version.createdAt()));
            publish(tenantId, projectId, documentId, userId, version);
            schedulePrune(tenantId, documentId);
            return document;
        } catch (RuntimeException exception) {
            compensate(key);
            throw exception;
        }
    }

    /** Modo append-only: crea una versión inmutable nueva y conserva la anterior. */
    @Transactional
    public DocumentVersion addVersion(UUID tenantId, UUID userId, UUID projectId, UUID documentId,
                                      AddVersionCommand command, FileUpload file) {
        Role role = currentRole();
        requireCanWrite(tenantId, projectId, userId, role);
        Document document = requireDocument(tenantId, projectId, documentId);
        String label = requireVersionLabel(command.versionLabel());
        if (repository.existsVersion(documentId, label)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una versión con esa etiqueta");
        }

        UUID versionId = UUID.randomUUID();
        String key = objectKey(tenantId, projectId, documentId, versionId, versionId, file.originalName());
        try {
            DocumentVersion version = storeVersion(tenantId, projectId, userId, documentId,
                    versionId, document.getKind(), label, file, key);
            repository.saveVersion(version);
            document.pointTo(version.id());
            repository.save(document);
            repository.recordChange(change(tenantId, documentId, version, version.createdAt()));
            publish(tenantId, projectId, documentId, userId, version);
            schedulePrune(tenantId, documentId);
            return version;
        } catch (RuntimeException exception) {
            compensate(key);
            throw exception;
        }
    }

    /**
     * Modo sobrescritura: reemplaza el contenido de la versión vigente en la
     * misma fila. Su identificador, etiqueta y fecha de creación se conservan.
     */
    @Transactional
    public DocumentVersion overwrite(UUID tenantId, UUID userId, UUID projectId, UUID documentId,
                                     FileUpload file) {
        Role role = currentRole();
        requireCanWrite(tenantId, projectId, userId, role);
        Document document = requireDocument(tenantId, projectId, documentId);
        DocumentVersion current = repository
                .findCurrentVersion(tenantId, documentId, document.getCurrentVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Versión vigente no encontrada"));
        LocalDateTime changedAt = LocalDateTime.now();
        String key = objectKey(tenantId, projectId, documentId, current.id(),
                UUID.randomUUID(), file.originalName());
        try {
            StoredBinary binary = storeBinary(document.getKind(), file, key);
            DocumentVersion updated = new DocumentVersion(current.id(), tenantId, documentId,
                    current.versionLabel(), key, binary.contentType(), binary.sizeBytes(),
                    binary.sha256(), binary.originalName(), userId, current.createdAt(),
                    changedAt, true);
            repository.updateVersion(updated);
            repository.recordChange(change(tenantId, documentId, updated, changedAt));
            publish(tenantId, projectId, documentId, userId, updated);
            schedulePrune(tenantId, documentId);
            return updated;
        } catch (RuntimeException exception) {
            compensate(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<Document> list(UUID tenantId, UUID userId, UUID projectId) {
        requireCanRead(tenantId, projectId, userId, currentRole());
        return repository.findAllByTenantIdAndProjectId(tenantId, projectId);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersion> versions(UUID tenantId, UUID userId, UUID projectId, UUID documentId) {
        requireCanRead(tenantId, projectId, userId, currentRole());
        requireDocument(tenantId, projectId, documentId);
        return repository.findVersions(tenantId, documentId);
    }

    @Transactional(readOnly = true)
    public DownloadLink download(UUID tenantId, UUID userId, UUID projectId,
                                 UUID documentId, UUID versionId) {
        requireCanRead(tenantId, projectId, userId, currentRole());
        requireDocument(tenantId, projectId, documentId);
        DocumentVersion version = repository.findVersion(tenantId, documentId, versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Versión no encontrada"));
        if (!version.binaryAvailable()) {
            throw new ResponseStatusException(HttpStatus.GONE,
                    "El binario de esta versión ya no está disponible");
        }
        return new DownloadLink(storage.presignDownload(version.objectKey(), version.originalName()),
                ObjectStoragePort.PRESIGN_TTL_SECONDS);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInTenant(UUID documentId, UUID tenantId) {
        return documentId != null && repository.existsByIdAndTenantId(documentId, tenantId);
    }

    /** Rol resuelto de la membresía activa, no el claim enviado en el token. */
    private Role currentRole() {
        return TenantContext.getCurrentRole().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "No hay un rol autenticado"));
    }

    /** ADMIN accede a todo; MEMBER y WORKER deben estar asignados al proyecto. */
    private void requireCanRead(UUID tenantId, UUID projectId, UUID userId, Role role) {
        projectTenantPort.ensureBelongsToTenant(projectId, tenantId);
        if (!role.isAdmin() && !projectTenantPort.isUserAssignedToProject(projectId, tenantId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
    }

    /** WORKER puede consultar, pero nunca cargar o versionar archivos técnicos. */
    private void requireCanWrite(UUID tenantId, UUID projectId, UUID userId, Role role) {
        if (role == Role.WORKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El rol WORKER solo puede consultar documentos");
        }
        requireCanRead(tenantId, projectId, userId, role);
    }

    private Document requireDocument(UUID tenantId, UUID projectId, UUID documentId) {
        return repository.findByIdAndTenantIdAndProjectId(documentId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Documento no encontrado"));
    }

    private String requireVersionLabel(String label) {
        if (label == null || !VERSION_LABEL.matcher(label.trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etiqueta de versión inválida");
        }
        return label.trim();
    }

    private DocumentVersion storeVersion(UUID tenantId, UUID projectId, UUID userId,
                                         UUID documentId, UUID versionId, AssetKind kind,
                                         String label, FileUpload file, String key) {
        StoredBinary binary = storeBinary(kind, file, key);
        return DocumentVersion.stored(versionId, tenantId, documentId, label, key,
                binary.contentType(), binary.sizeBytes(), binary.sha256(), binary.originalName(),
                userId, LocalDateTime.now());
    }

    private StoredBinary storeBinary(AssetKind kind, FileUpload file, String key) {
        MessageDigest digest = sha256Digest();
        byte[] header = new byte[HEADER_BYTES];
        try (PushbackInputStream probe = new PushbackInputStream(file.content(), HEADER_BYTES);
             DigestInputStream content = new DigestInputStream(probe, digest)) {
            int read = probe.read(header);
            FilePolicy.validate(kind, file.originalName(), file.contentType(),
                    file.size(), read < 0 ? new byte[0] : Arrays.copyOf(header, read));
            if (read > 0) {
                probe.unread(header, 0, read);
            }
            storage.store(key, content, file.size(), safeContentType(file.contentType()));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo leer el archivo", exception);
        }
        return new StoredBinary(safeContentType(file.contentType()), file.size(),
                HexFormat.of().formatHex(digest.digest()), safeFileName(file.originalName()));
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", exception);
        }
    }

    private void publish(UUID tenantId, UUID projectId, UUID documentId, UUID userId,
                         DocumentVersion version) {
        events.publishEvent(new DocumentVersionUploaded(UUID.randomUUID(), tenantId, projectId,
                documentId, version.id(), userId, version.versionLabel(), version.originalName(),
                version.sizeBytes(), LocalDateTime.now()));
    }

    /**
     * El podado no puede ejecutarse dentro de la transacción: si esta se
     * revertiera, los objetos borrados no se podrían recuperar. Por eso se
     * programa después del commit y falla de forma no bloqueante.
     */
    private void schedulePrune(UUID tenantId, UUID documentId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                prune(tenantId, documentId);
            }
        });
    }

    /** Conserva los últimos cambios del documento y poda el resto del almacenamiento. */
    public void prune(UUID tenantId, UUID documentId) {
        List<DocumentVersionChange> older = repository.listChanges(tenantId, documentId).stream()
                .sorted(Comparator.comparing(DocumentVersionChange::changedAt)
                        .thenComparing(DocumentVersionChange::id).reversed())
                .skip(VERSION_RETENTION)
                .filter(change -> change.prunedAt() == null)
                .toList();
        for (DocumentVersionChange change : older) {
            try {
                storage.delete(change.objectKey());
                repository.markChangePruned(tenantId, change.id(), LocalDateTime.now());
            } catch (RuntimeException exception) {
                log.warn("No se pudo podar el objeto {} del documento {}",
                        change.objectKey(), documentId, exception);
            }
        }
    }

    /**
     * Si la transacción falla, la metadata nunca se consolida: PostgreSQL la
     * descarta por rollback. Solo hay que compensar el objeto ya subido a S3.
     */
    private void compensate(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("No se pudo eliminar el objeto {} tras un fallo de persistencia", key, exception);
        }
    }

    private DocumentVersionChange change(UUID tenantId, UUID documentId,
                                         DocumentVersion version, LocalDateTime changedAt) {
        return new DocumentVersionChange(UUID.randomUUID(), tenantId, documentId,
                version.id(), version.objectKey(), changedAt, null);
    }

    private String objectKey(UUID tenantId, UUID projectId, UUID documentId,
                             UUID versionId, UUID changeId, String originalName) {
        return "tenants/" + tenantId + "/projects/" + projectId + "/documents/"
                + documentId + "/" + versionId + "/" + changeId + "/" + safeFileName(originalName);
    }

    /** Nombre seguro para la clave S3 y para el Content-Disposition de descarga. */
    private String safeFileName(String originalName) {
        String name = originalName == null ? "archivo" : originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        name = name.replaceAll("_{2,}", "_");
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        if (name.isBlank()) {
            name = "archivo";
        }
        return name.length() > 100 ? name.substring(name.length() - 100) : name;
    }

    private String safeContentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }

    private record StoredBinary(String contentType, long sizeBytes, String sha256, String originalName) { }
}
