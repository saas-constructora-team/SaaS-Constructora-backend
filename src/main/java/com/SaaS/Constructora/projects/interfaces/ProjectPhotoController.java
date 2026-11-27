package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.documents.application.ports.FileUpload;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.domain.ProjectPhoto;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** API REST de evidencia fotográfica de etapas de proyecto. */
@RestController
@RequestMapping("/api/projects/{projectId}/stages/{stageId}/photos")
public class ProjectPhotoController {

    private final ProjectService service;

    ProjectPhotoController(ProjectService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProjectPhotoResponse> upload(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID stageId,
            @RequestPart("file") MultipartFile file) {
        ProjectPhoto photo = service.registerPhoto(user.tenantId(), projectId, stageId,
                user.userId(), user.role(), toFileUpload(file));
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectPhotoResponse.from(photo));
    }

    @GetMapping
    List<ProjectPhotoResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID stageId) {
        return service.listPhotos(user.tenantId(), projectId, stageId,
                        user.userId(), user.role())
                .stream()
                .map(ProjectPhotoResponse::from)
                .toList();
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
