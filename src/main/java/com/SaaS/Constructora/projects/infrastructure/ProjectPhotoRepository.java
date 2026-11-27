package com.SaaS.Constructora.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Metadatos de evidencia fotográfica, siempre filtrados por tenant. */
public interface ProjectPhotoRepository extends JpaRepository<ProjectPhotoEntity, UUID> {

    List<ProjectPhotoEntity> findByTenantIdAndProjectIdAndStageIdOrderByCreatedAtDesc(
            UUID tenantId, UUID projectId, UUID stageId);
}
