package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.application.ProjectStageService;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cambio de color de una columna del tablero. Las etapas pueden pertenecer a
 * varios proyectos, por lo que el proyecto se resuelve en el cuerpo y se valida
 * en el caso de uso con el tenant y los permisos del usuario autenticado.
 */
@RestController
@RequestMapping("/api/projects/stages")
public class ProjectStageColorsController {

    private final ProjectStageService service;

    ProjectStageColorsController(ProjectStageService service) {
        this.service = service;
    }

    @PutMapping("/color")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void updateColors(@CurrentUser AuthenticatedUser user,
                      @Valid @RequestBody UpdateStageColorsRequest request) {
        service.updateColors(user.tenantId(), user.userId(), user.role(), request);
    }
}
