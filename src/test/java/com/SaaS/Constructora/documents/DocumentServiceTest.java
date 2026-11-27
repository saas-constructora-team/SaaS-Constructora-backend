package com.SaaS.Constructora.documents;

import com.SaaS.Constructora.documents.application.AddVersionCommand;
import com.SaaS.Constructora.documents.application.DocumentService;
import com.SaaS.Constructora.documents.application.DownloadLink;
import com.SaaS.Constructora.documents.application.UploadDocumentCommand;
import com.SaaS.Constructora.documents.application.ports.DocumentRepository;
import com.SaaS.Constructora.documents.application.ports.FileUpload;
import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import com.SaaS.Constructora.documents.domain.AssetKind;
import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;
import com.SaaS.Constructora.documents.domain.DocumentVersionChange;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.shared.events.DocumentVersionUploaded;
import com.SaaS.Constructora.shared.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4, 5, 6, 7, 8
    };

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    private final DocumentRepository repository = mock(DocumentRepository.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final ProjectTenantPort projects = mock(ProjectTenantPort.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final DocumentService service =
            new DocumentService(repository, storage, projects, events);

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(tenantId);
        TenantContext.setCurrentRole(Role.ADMIN);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void adminCargaLaPrimeraVersionConClavePorTenantYProyecto() {
        when(repository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.saveVersion(any(DocumentVersion.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.recordChange(any(DocumentVersionChange.class))).thenAnswer(i -> i.getArgument(0));

        Document document = service.upload(tenantId, userId, projectId,
                new UploadDocumentCommand("Planta arquitectónica", AssetKind.IMAGE, "v1.0"),
                pngUpload());

        assertNotNull(document.getCurrentVersionId());
        assertEquals(tenantId, document.getTenantId());
        verify(projects).ensureBelongsToTenant(projectId, tenantId);
        verify(storage).store(startsWith("tenants/" + tenantId + "/projects/" + projectId),
                any(InputStream.class), eq((long) PNG.length), eq("image/png"));
        verify(repository).recordChange(any(DocumentVersionChange.class));
        verify(events).publishEvent(any(DocumentVersionUploaded.class));
    }

    @Test
    void workerNoPuedeCargarDocumentos() {
        TenantContext.setCurrentRole(Role.WORKER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.upload(tenantId, userId, projectId,
                        new UploadDocumentCommand("Planta", AssetKind.IMAGE, "v1.0"), pngUpload()));

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getStatusCode().value());
        verifyNoInteractions(storage);
    }

    @Test
    void memberNoAsignadoNoPuedeCargar() {
        TenantContext.setCurrentRole(Role.MEMBER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.upload(tenantId, userId, projectId,
                        new UploadDocumentCommand("Planta", AssetKind.IMAGE, "v1.0"), pngUpload()));

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getStatusCode().value());
        verify(projects).isUserAssignedToProject(projectId, tenantId, userId);
        verifyNoInteractions(storage);
    }

    @Test
    void proyectoDeOtroTenantSeReportaComoInexistente() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projects).ensureBelongsToTenant(projectId, tenantId);

        assertThrows(ResponseStatusException.class,
                () -> service.upload(tenantId, userId, projectId,
                        new UploadDocumentCommand("Planta", AssetKind.IMAGE, "v1.0"), pngUpload()));

        verifyNoInteractions(storage);
    }

    @Test
    void etiquetaDeVersionRepetidaEsConflictoYSubeNada() {
        Document document = existingDocument();
        when(repository.findByIdAndTenantIdAndProjectId(document.getId(), tenantId, projectId))
                .thenReturn(Optional.of(document));
        when(repository.existsVersion(document.getId(), "v1.0")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addVersion(tenantId, userId, projectId, document.getId(),
                        new AddVersionCommand("v1.0"), pngUpload()));

        assertEquals(HttpStatus.CONFLICT.value(), error.getStatusCode().value());
        verifyNoInteractions(storage);
    }

    @Test
    void nuevaVersionActualizaElPunteroYConservaElHistorial() {
        Document document = existingDocument();
        when(repository.findByIdAndTenantIdAndProjectId(document.getId(), tenantId, projectId))
                .thenReturn(Optional.of(document));
        when(repository.saveVersion(any(DocumentVersion.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.recordChange(any(DocumentVersionChange.class))).thenAnswer(i -> i.getArgument(0));

        DocumentVersion version = service.addVersion(tenantId, userId, projectId, document.getId(),
                new AddVersionCommand("v2.0"), pngUpload());

        assertEquals("v2.0", version.versionLabel());
        assertEquals(version.id(), document.getCurrentVersionId());
        verify(storage).store(startsWith("tenants/" + tenantId), any(InputStream.class),
                eq((long) PNG.length), eq("image/png"));
        verify(repository).recordChange(any(DocumentVersionChange.class));
    }

    @Test
    void sobrescribeLaVersionVigenteSinCrearOtra() {
        Document document = existingDocumentWithVersion();
        DocumentVersion current = DocumentVersion.stored(document.getCurrentVersionId(), tenantId,
                document.getId(), "v1.0", "clave-vieja", "image/png", PNG.length, "abc",
                "planta.png", userId, LocalDateTime.now().minusDays(1));
        when(repository.findByIdAndTenantIdAndProjectId(document.getId(), tenantId, projectId))
                .thenReturn(Optional.of(document));
        when(repository.findCurrentVersion(tenantId, document.getId(), document.getCurrentVersionId()))
                .thenReturn(Optional.of(current));
        when(repository.updateVersion(any(DocumentVersion.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.recordChange(any(DocumentVersionChange.class))).thenAnswer(i -> i.getArgument(0));

        DocumentVersion updated = service.overwrite(tenantId, userId, projectId,
                document.getId(), pngUpload());

        assertEquals(current.id(), updated.id());
        assertEquals("v1.0", updated.versionLabel());
        assertEquals(current.createdAt(), updated.createdAt());
        assertTrue(updated.changedAt().isAfter(current.createdAt()));
        assertNotEquals(current.objectKey(), updated.objectKey());
        assertEquals(userId, updated.uploadedBy());
        verify(repository, never()).saveVersion(any(DocumentVersion.class));
        verify(repository).updateVersion(any(DocumentVersion.class));
        verify(repository).recordChange(any(DocumentVersionChange.class));
        verify(storage).store(startsWith("tenants/" + tenantId), any(InputStream.class),
                eq((long) PNG.length), eq("image/png"));
        verify(events).publishEvent(any(DocumentVersionUploaded.class));
    }

    @Test
    void workerNoPuedeSobrescribir() {
        TenantContext.setCurrentRole(Role.WORKER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.overwrite(tenantId, userId, projectId,
                        UUID.randomUUID(), pngUpload()));

        assertEquals(HttpStatus.FORBIDDEN.value(), error.getStatusCode().value());
        verifyNoInteractions(storage);
    }

    @Test
    void documentoDeOtroTenantNoSePuedeSobrescribir() {
        UUID documentId = UUID.randomUUID();
        when(repository.findByIdAndTenantIdAndProjectId(documentId, tenantId, projectId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.overwrite(tenantId, userId, projectId, documentId, pngUpload()));

        assertEquals(HttpStatus.NOT_FOUND.value(), error.getStatusCode().value());
        verifyNoInteractions(storage);
    }

    @Test
    void falloDePersistenciaEnSobrescrituraCompensaElObjetoSubido() {
        Document document = existingDocumentWithVersion();
        DocumentVersion current = DocumentVersion.stored(document.getCurrentVersionId(), tenantId,
                document.getId(), "v1.0", "clave-vieja", "image/png", PNG.length, "abc",
                "planta.png", userId, LocalDateTime.now());
        when(repository.findByIdAndTenantIdAndProjectId(document.getId(), tenantId, projectId))
                .thenReturn(Optional.of(document));
        when(repository.findCurrentVersion(tenantId, document.getId(), document.getCurrentVersionId()))
                .thenReturn(Optional.of(current));
        doThrow(new DataIntegrityViolationException("fallo de BD"))
                .when(repository).updateVersion(any(DocumentVersion.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> service.overwrite(tenantId, userId, projectId, document.getId(), pngUpload()));

        verify(storage).store(anyString(), any(InputStream.class), eq((long) PNG.length), anyString());
        verify(storage).delete(anyString());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void podadoConservaLosUltimosTresCambios() {
        UUID documentId = UUID.randomUUID();
        List<DocumentVersionChange> changes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            changes.add(new DocumentVersionChange(UUID.randomUUID(), tenantId, documentId,
                    UUID.randomUUID(), "clave-" + i,
                    LocalDateTime.now().minusMinutes(5L - i), null));
        }
        when(repository.listChanges(tenantId, documentId)).thenReturn(changes);

        service.prune(tenantId, documentId);

        verify(storage).delete("clave-1");
        verify(storage).delete("clave-2");
        verify(storage, never()).delete("clave-3");
        verify(storage, never()).delete("clave-4");
        verify(storage, never()).delete("clave-5");
        verify(repository).markChangePruned(eq(tenantId), eq(changes.get(0).id()), any(LocalDateTime.class));
        verify(repository).markChangePruned(eq(tenantId), eq(changes.get(1).id()), any(LocalDateTime.class));
    }

    @Test
    void falloAlPodarNoRevierteLaCarga() {
        UUID documentId = UUID.randomUUID();
        DocumentVersionChange old = new DocumentVersionChange(UUID.randomUUID(), tenantId, documentId,
                UUID.randomUUID(), "clave-vieja", LocalDateTime.now().minusDays(1), null);
        when(repository.listChanges(tenantId, documentId)).thenReturn(List.of(old));
        doThrow(new IllegalStateException("S3 caído")).when(storage).delete("clave-vieja");

        service.prune(tenantId, documentId);

        verify(repository, never()).markChangePruned(any(), any(), any());
    }

    @Test
    void consultaHistorialYDevuelveUrlFirmada() {
        Document document = existingDocument();
        DocumentVersion version = DocumentVersion.stored(UUID.randomUUID(), tenantId, document.getId(),
                "v1.0", "clave", "image/png", PNG.length, "abc", "planta.png",
                userId, LocalDateTime.now());
        when(repository.findByIdAndTenantIdAndProjectId(document.getId(), tenantId, projectId))
                .thenReturn(Optional.of(document));
        when(repository.findVersions(tenantId, document.getId())).thenReturn(List.of(version));
        when(repository.findVersion(tenantId, document.getId(), version.id()))
                .thenReturn(Optional.of(version));
        when(storage.presignDownload("clave", "planta.png")).thenReturn("https://firmada");

        assertEquals(1, service.versions(tenantId, userId, projectId, document.getId()).size());
        DownloadLink link = service.download(tenantId, userId, projectId,
                document.getId(), version.id());

        assertEquals("https://firmada", link.url());
        assertEquals(900L, link.expiresInSeconds());
        assertTrue(link.url().startsWith("https://"));
    }

    @Test
    void descargaDeUnaVersionPodadaEsGone() {
        Document document = existingDocument();
        DocumentVersion version = DocumentVersion.stored(UUID.randomUUID(), tenantId, document.getId(),
                "v1.0", "clave", "image/png", PNG.length, "abc", "planta.png",
                userId, LocalDateTime.now()).withoutBinary();
        when(repository.findByIdAndTenantIdAndProjectId(document.getId(), tenantId, projectId))
                .thenReturn(Optional.of(document));
        when(repository.findVersion(tenantId, document.getId(), version.id()))
                .thenReturn(Optional.of(version));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.download(tenantId, userId, projectId, document.getId(), version.id()));

        assertEquals(HttpStatus.GONE.value(), error.getStatusCode().value());
        verify(storage, never()).presignDownload(anyString(), anyString());
    }

    private Document existingDocument() {
        return Document.rehydrate(UUID.randomUUID(), tenantId, projectId, AssetKind.IMAGE,
                "Planta", null, userId, LocalDateTime.now());
    }

    private Document existingDocumentWithVersion() {
        return Document.rehydrate(UUID.randomUUID(), tenantId, projectId, AssetKind.IMAGE,
                "Planta", UUID.randomUUID(), userId, LocalDateTime.now());
    }

    private FileUpload pngUpload() {
        return new FileUpload(new ByteArrayInputStream(PNG), "planta.png", "image/png", PNG.length);
    }
}
