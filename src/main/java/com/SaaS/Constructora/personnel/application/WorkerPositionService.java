package com.SaaS.Constructora.personnel.application;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPosition;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPositionRepository;
import com.SaaS.Constructora.personnel.infrastructure.WorkerRepository;
import com.SaaS.Constructora.personnel.interfaces.CreateWorkerPositionRequest;
import com.SaaS.Constructora.personnel.interfaces.UpdateWorkerPositionRequest;
import com.SaaS.Constructora.personnel.interfaces.WorkerPositionResponse;

import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de gestión de cargos (posiciones) de la empresa.
 * <p>
 * Toda operación se acota al tenant del token y exige el mismo permiso que la
 * gestión de personal: un ADMIN o un MEMBER al que un ADMIN le haya concedido
 * el permiso de personal.
 */
@Service
public class WorkerPositionService {

    private final WorkerPositionRepository positionRepository;
    private final WorkerRepository workerRepository;
    private final PersonnelPermissionService permissionService;

    public WorkerPositionService(WorkerPositionRepository positionRepository,
                                 WorkerRepository workerRepository,
                                 PersonnelPermissionService permissionService) {
        this.positionRepository = positionRepository;
        this.workerRepository = workerRepository;
        this.permissionService = permissionService;
    }

    /** Crea un nuevo cargo en el tenant autenticado. */
    @Transactional
    public WorkerPositionResponse create(UUID tenantId, UUID actorId,
                                         CreateWorkerPositionRequest request) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        String name = requireText(request.name());
        if (positionRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un cargo con el nombre '" + name + "' en la empresa");
        }

        WorkerPosition position = WorkerPosition.create(
                tenantId, name, optionalText(request.description()));
        return WorkerPositionResponse.from(positionRepository.save(position));
    }

    /** Lista todos los cargos del tenant, ordenados alfabéticamente por nombre. */
    @Transactional(readOnly = true)
    public List<WorkerPositionResponse> list(UUID tenantId, UUID actorId) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);
        return positionRepository.findByTenantIdOrderByNameAsc(tenantId)
                .stream()
                .map(WorkerPositionResponse::from)
                .toList();
    }

    /**
     * Actualiza un cargo existente. Si cambia el nombre, también se actualiza
     * el cargo de los trabajadores del tenant que lo tenían asignado.
     */
    @Transactional
    public WorkerPositionResponse update(UUID tenantId, UUID actorId, UUID id,
                                         UpdateWorkerPositionRequest request) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        WorkerPosition position = find(tenantId, id);

        if (request.name() != null) {
            String newName = requireText(request.name());
            if (!newName.equals(position.getName())) {
                if (positionRepository.existsByTenantIdAndNameAndIdNot(
                        tenantId, newName, id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe un cargo con el nombre '" + newName + "' en la empresa");
                }
                workerRepository.renamePosition(tenantId, position.getName(), newName);
                position.rename(newName);
            }
        }

        if (request.description() != null) {
            position.describe(optionalText(request.description()));
        }

        if (request.active() != null) {
            position.activate(request.active());
        }

        return WorkerPositionResponse.from(positionRepository.save(position));
    }

    /**
     * Elimina un cargo. No se permite eliminar un cargo que tenga trabajadores
     * asignados; en ese caso se puede desactivar o reasignar primero.
     */
    @Transactional
    public void delete(UUID tenantId, UUID actorId, UUID id) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        WorkerPosition position = find(tenantId, id);

        if (workerRepository.existsByTenantIdAndPositionName(tenantId, position.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el cargo '" + position.getName()
                            + "' porque tiene trabajadores asignados. "
                            + "Desactívelo o reasigne primero a esos trabajadores.");
        }

        positionRepository.delete(position);
    }

    private WorkerPosition find(UUID tenantId, UUID id) {
        return positionRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cargo no encontrado"));
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre del cargo es obligatorio");
        }
        return value.trim();
    }

    /** Normaliza los datos opcionales: recorta y convierte vacíos en null. */
    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
