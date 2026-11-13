package com.SaaS.Constructora.personnel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface WorkerRepository extends JpaRepository<Worker, UUID> {

    List<Worker> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    boolean existsByTenantIdAndDocumentTypeAndDocumentNumber(UUID tenantId,
                                                             DocumentType documentType,
                                                             String documentNumber);

    java.util.Optional<Worker> findByTenantIdAndDocumentTypeAndDocumentNumber(
            UUID tenantId, DocumentType documentType, String documentNumber);

    boolean existsByTenantIdAndPositionName(UUID tenantId, String positionName);

    /** Renombra el cargo en todos los trabajadores del tenant que lo tenían. */
    @Modifying(clearAutomatically = true)
    @Query("""
            update Worker w
               set w.positionName = :newName
             where w.tenantId = :tenantId
               and w.positionName = :oldName
            """)
    int renamePosition(@Param("tenantId") UUID tenantId,
                       @Param("oldName") String oldName,
                       @Param("newName") String newName);
}
