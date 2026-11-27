package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.domain.MembershipStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usuarios con membresía activa en el tenant, en una sola consulta. */
    @Query("""
            select u.id as id, u.email as email, u.fullName as fullName,
                   m.role as role, u.active as active,
                   m.canCreatePersonnel as canCreatePersonnel,
                   m.canViewAssignmentCandidates as canViewAssignmentCandidates,
                   m.canCreateTasks as canCreateTasks,
                   m.canManageSiteLog as canManageSiteLog
            from MembershipJpaEntity m join UserJpaEntity u on u.id = m.userId
            where m.tenantId = :tenantId and m.status = :status and m.active = true
            order by u.fullName asc
            """)
    List<ActiveUserProjection> findActiveByTenantId(
            @Param("tenantId") UUID tenantId, @Param("status") MembershipStatus status);

    /** Same projection limited to the ids supplied by the caller. */
    @Query("""
            select u.id as id, u.email as email, u.fullName as fullName,
                   m.role as role, u.active as active,
                   m.canCreatePersonnel as canCreatePersonnel,
                   m.canViewAssignmentCandidates as canViewAssignmentCandidates,
                   m.canCreateTasks as canCreateTasks,
                   m.canManageSiteLog as canManageSiteLog
            from MembershipJpaEntity m join UserJpaEntity u on u.id = m.userId
            where m.tenantId = :tenantId and m.status = :status and m.active = true
              and u.id in :userIds
            order by u.fullName asc
            """)
    List<ActiveUserProjection> findActiveByTenantIdAndUserIdIn(
            @Param("tenantId") UUID tenantId, @Param("status") MembershipStatus status,
            @Param("userIds") List<UUID> userIds);
}
